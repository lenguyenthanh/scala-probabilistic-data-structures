package se.thanh.pds.bloomfilter

import se.thanh.pds.bloomfilter.HashSubprocessSpec.*
import se.thanh.pds.bloomfilter.internal.hashing.{ DefaultStringHash, MurmurHash3 }

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

class HashSubprocessSpec extends munit.FunSuite:

  private val javaFeature = Runtime.version().feature()
  private val modes       =
    if javaFeature >= 23 then UnsafeMode.values.toList
    else List(UnsafeMode.Default)
  private val configurations =
    for
      mode     <- modes
      addOpens <- List(false, true)
    yield JvmConfiguration(javaFeature, mode, addOpens)

  for
    configuration <- configurations
    target        <- HashTarget.values
  do
    test(s"${configuration.name}: ${target.argument}"):
      verify(configuration, target, runProbe(configuration.options, target))

  private def verify(
      configuration: JvmConfiguration,
      target: HashTarget,
      result: ProbeResult
  ): Unit =
    val expectedAcquisition = target match
      case HashTarget.Default | HashTarget.Safe => true
      case HashTarget.Unsafe                    => configuration.unsafeAvailable
      case HashTarget.VarHandle                 => configuration.addOpens

    assertEquals(
      result.fields.get("HASH_ACQUISITION"),
      Some(if expectedAcquisition then "success" else "failure"),
      result.clue
    )

    if expectedAcquisition then
      assertEquals(result.fields.get("HASH_FIRST_HASH"), Some("success"), result.clue)
      assertEquals(result.fields.get("HASH_RUNTIME"), Some("success"), result.clue)
      assertEquals(
        result.fields.get("HASH_SEMANTICS"),
        Some(expectedSemantics(configuration, target)),
        result.clue
      )
      if target == HashTarget.Default then
        assertEquals(
          result.fields.get("HASH_DEFAULT_IMPLEMENTATION"),
          Some(configuration.defaultImplementation),
          result.clue
        )
    else
      assertEquals(result.fields.get("HASH_FIRST_HASH"), Some("not-run"), result.clue)
      assertEquals(result.fields.get("HASH_RUNTIME"), Some("not-run"), result.clue)
      assert(
        result.fields.get("HASH_FAILURE").exists(_.contains(expectedFailureMessage(target))),
        result.clue
      )

    verifyUnsafeDiagnostics(configuration, target, result)

  private def expectedSemantics(
      configuration: JvmConfiguration,
      target: HashTarget
  ): String =
    target match
      case HashTarget.Safe                                                     => "safe"
      case HashTarget.Default if configuration.defaultImplementation == "Safe" => "safe"
      case _                                                                   => "compact"

  private def expectedFailureMessage(target: HashTarget): String =
    target match
      case HashTarget.Unsafe    => "Hash.unsafe is unavailable"
      case HashTarget.VarHandle => "Hash.privateJDK requires private JDK access"
      case _                    => fail(s"$target is not expected to fail during acquisition")

  private def verifyUnsafeDiagnostics(
      configuration: JvmConfiguration,
      target: HashTarget,
      result: ProbeResult
  ): Unit =
    val usesAvailableUnsafe =
      configuration.unsafeAvailable &&
        (target == HashTarget.Default || target == HashTarget.Unsafe)
    val expectedDiagnostic =
      if usesAvailableUnsafe then configuration.unsafeDiagnostic
      else UnsafeDiagnostic.None
    val mentionsHasher = result.stderr.linesIterator.exists(_.contains("UnsafeStringHash"))
    val hasStackTrace  = result.stderr.linesIterator.exists: line =>
      line.trim.startsWith("at se.thanh.pds.bloomfilter.internal.hashing.UnsafeStringHash")

    expectedDiagnostic match
      case UnsafeDiagnostic.None =>
        assert(!mentionsHasher, result.clue)
      case UnsafeDiagnostic.Warning =>
        assert(mentionsHasher, result.clue)
        assert(!hasStackTrace, result.clue)
      case UnsafeDiagnostic.Debug =>
        assert(mentionsHasher, result.clue)
        assert(hasStackTrace, result.clue)

  private def runProbe(
      options: List[String],
      target: HashTarget
  ): ProbeResult =
    val javaExecutable =
      java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString
    val command =
      javaExecutable :: options ::: List(
        "-cp",
        System.getProperty("java.class.path"),
        HashSubprocessProbe.getClass.getName.stripSuffix("$"),
        target.argument
      )
    val process      = new ProcessBuilder(command.asJava).start()
    val stdoutBuffer = new ByteArrayOutputStream()
    val stderrBuffer = new ByteArrayOutputStream()
    val stdoutReader = streamReader(process.getInputStream, stdoutBuffer)
    val stderrReader = streamReader(process.getErrorStream, stderrBuffer)
    val exited       = process.waitFor(30, TimeUnit.SECONDS)
    if !exited then
      process.destroyForcibly()
      fail(s"probe did not exit: ${command.mkString(" ")}")
    stdoutReader.join()
    stderrReader.join()

    val stdout = new String(stdoutBuffer.toByteArray, StandardCharsets.UTF_8)
    val stderr = new String(stderrBuffer.toByteArray, StandardCharsets.UTF_8)
    val exit   = process.exitValue()
    assertEquals(exit, 0, s"stdout:\n$stdout\nstderr:\n$stderr")

    val fields = stdout.linesIterator.flatMap: line =>
      line.split("=", 2) match
        case Array(key, value) if key.startsWith("HASH_") => Some(key -> value)
        case _                                            => None
    ProbeResult(fields.toMap, stdout, stderr)

  private def streamReader(
      input: java.io.InputStream,
      output: ByteArrayOutputStream
  ): Thread =
    val thread = new Thread(() =>
      input.transferTo(output)
      ()
    )
    thread.setDaemon(true)
    thread.start()
    thread

object HashSubprocessSpec:

  private enum UnsafeMode(val argument: Option[String]):
    case Default extends UnsafeMode(None)
    case Allow   extends UnsafeMode(Some("allow"))
    case Warn    extends UnsafeMode(Some("warn"))
    case Debug   extends UnsafeMode(Some("debug"))
    case Deny    extends UnsafeMode(Some("deny"))

  private enum UnsafeDiagnostic:
    case None, Warning, Debug

  private enum HashTarget(val argument: String):
    case Default   extends HashTarget("default")
    case Unsafe    extends HashTarget("unsafe")
    case VarHandle extends HashTarget("varhandle")
    case Safe      extends HashTarget("safe")

  final private case class JvmConfiguration(
      javaFeature: Int,
      mode: UnsafeMode,
      addOpens: Boolean
  ):
    val name: String =
      s"JDK $javaFeature, unsafe=${mode.toString.toLowerCase}, add-opens=$addOpens"

    val options: List[String] =
      mode.argument.toList.map(value => s"--sun-misc-unsafe-memory-access=$value") ++
        Option.when(addOpens)("--add-opens=java.base/java.lang=ALL-UNNAMED")

    val unsafeAvailable: Boolean = mode != UnsafeMode.Deny

    val defaultImplementation: String =
      if unsafeAvailable then "Unsafe"
      else if addOpens then "VarHandle"
      else "Safe"

    val unsafeDiagnostic: UnsafeDiagnostic =
      mode match
        case UnsafeMode.Default if javaFeature >= 24 => UnsafeDiagnostic.Warning
        case UnsafeMode.Warn                         => UnsafeDiagnostic.Warning
        case UnsafeMode.Debug                        => UnsafeDiagnostic.Debug
        case _                                       => UnsafeDiagnostic.None

  final private case class ProbeResult(
      fields: Map[String, String],
      stdout: String,
      stderr: String
  ):
    def clue: String = s"stdout:\n$stdout\nstderr:\n$stderr"

private object HashSubprocessProbe:

  private val values = List(
    "hello world",
    "h\u00e9llo \u00ff",
    "a\u0000b",
    "\u65e5\u672c\u8a9e",
    "x\ud83d\ude00y",
    "\ud800",
    "\udc00"
  )

  def main(args: Array[String]): Unit =
    require(args.length == 1, "expected one hash target")
    try
      val hash = acquire(args(0))
      println("HASH_ACQUISITION=success")
      verifyFirstHash(hash, args(0))
    catch
      case cause: LinkageError => reportAcquisitionFailure(cause)
      case NonFatal(cause)     => reportAcquisitionFailure(cause)

  private def acquire(target: String): Hash[String] =
    target match
      case "default"   => Hash[String]
      case "unsafe"    => Hash.unsafe.stringHash
      case "varhandle" => Hash.privateJDK.stringHash
      case "safe"      => Hash.safe.stringHash
      case other       => throw new IllegalArgumentException(s"unknown hash target: $other")

  private def verifyFirstHash(hash: Hash[String], target: String): Unit =
    try
      hash.hash(values.head)
      println("HASH_FIRST_HASH=success")
      if target == "default" then println(s"HASH_DEFAULT_IMPLEMENTATION=${DefaultStringHash.implementation}")
      verifyRepeatedHashing(hash)
    catch
      case cause: LinkageError => reportRuntimeFailure("HASH_FIRST_HASH", cause)
      case NonFatal(cause)     => reportRuntimeFailure("HASH_FIRST_HASH", cause)

  private def verifyRepeatedHashing(hash: Hash[String]): Unit =
    try
      val observed       = values.map(value => value -> hash.hash(value))
      val compactMatches = observed.forall: (value, actual) =>
        actual == hashBytes(compactBytes(value))
      val safeMatches = observed.forall: (value, actual) =>
        actual == hashBytes(utf16LeBytes(value))
      val semantics =
        if compactMatches && !safeMatches then "compact"
        else if safeMatches && !compactMatches then "safe"
        else "unknown"
      require(semantics != "unknown", "hash does not match exactly one String representation")

      var repetition = 0
      while repetition < 100 do
        observed.foreach: (value, expected) =>
          require(hash.hash(value) == expected, s"unstable hash for value $value")
        repetition += 1

      println(s"HASH_SEMANTICS=$semantics")
      println("HASH_RUNTIME=success")
    catch
      case cause: LinkageError => reportRuntimeFailure("HASH_RUNTIME", cause)
      case NonFatal(cause)     => reportRuntimeFailure("HASH_RUNTIME", cause)

  private def reportAcquisitionFailure(cause: Throwable): Unit =
    println("HASH_ACQUISITION=failure")
    println("HASH_FIRST_HASH=not-run")
    println("HASH_RUNTIME=not-run")
    println(s"HASH_FAILURE=${failureChain(cause)}")

  private def reportRuntimeFailure(stage: String, cause: Throwable): Unit =
    println(s"$stage=failure")
    if stage == "HASH_FIRST_HASH" then println("HASH_RUNTIME=not-run")
    println(s"HASH_FAILURE=${failureChain(cause)}")

  private def failureChain(cause: Throwable): String =
    val result  = new StringBuilder()
    var current = cause
    while current != null do
      if result.nonEmpty then result.append(" -> ")
      result.append(current.getClass.getName)
      Option(current.getMessage).foreach: message =>
        result.append(": ").append(message.replace('\n', ' ').replace('\r', ' '))
      current = current.getCause
    result.result()

  private def hashBytes(bytes: Array[Byte]): Long =
    MurmurHash3.murmurhash3_x64_64(bytes, 0, bytes.length, 0)

  private def compactBytes(value: String): Array[Byte] =
    if value.forall(_ <= 0xff) then value.map(_.toByte).toArray
    else utf16LeBytes(value)

  private def utf16LeBytes(value: String): Array[Byte] =
    val bytes = new Array[Byte](value.length * 2)
    var i     = 0
    while i < value.length do
      val codeUnit = value.charAt(i)
      bytes(i * 2) = codeUnit.toByte
      bytes(i * 2 + 1) = (codeUnit >>> 8).toByte
      i += 1
    bytes
