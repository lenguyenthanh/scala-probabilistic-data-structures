package se.thanh.pds.bloomfilter.internal.hashing

import se.thanh.pds.bloomfilter.Hash

import java.lang.invoke.{ MethodHandles, VarHandle }
import scala.util.control.NonFatal

private[bloomfilter] object PrivateStringHash:

  private inline val RequiredFlag =
    "--add-opens=java.base/java.lang=ALL-UNNAMED"

  val instance: Hash[String] = initialize()

  private def initialize(): Hash[String] =

    val y =
      try MethodHandles.privateLookupIn(classOf[String], MethodHandles.lookup())
      catch
        case NonFatal(cause) =>
          throw new IllegalStateException(
            s"Hash.UnsafeCompact requires private JDK access. Start the JVM with $RequiredFlag.",
            cause
          )

    val stringValue: VarHandle =
      try y.findVarHandle(classOf[String], "value", classOf[Array[Byte]])
      catch
        case NonFatal(cause) =>
          throw new IllegalStateException(
            "Hash.UnsafeCompact is incompatible with this JDK's String layout.",
            cause
          )

    new PrivateStringHash(stringValue)

  final private class PrivateStringHash(stringValue: VarHandle) extends Hash[String]:
    override def hash(from: String): Long =
      val value = stringValue.get(from).asInstanceOf[Array[Byte]]
      MurmurHash3.murmurhash3_x64_64(value, 0, value.length, 0)
