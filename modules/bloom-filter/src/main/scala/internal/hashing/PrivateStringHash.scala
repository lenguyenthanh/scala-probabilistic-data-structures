package se.thanh.pds.bloomfilter.internal.hashing

import se.thanh.pds.bloomfilter.Hash

import java.lang.invoke.{ MethodHandles, VarHandle }
import scala.util.control.NonFatal

private[bloomfilter] object PrivateStringHash:

  def instance: Either[Throwable, Hash[String]] =
    try
      val lookup =
        MethodHandles.privateLookupIn(classOf[String], MethodHandles.lookup())
      val stringValue: VarHandle =
        lookup.findVarHandle(classOf[String], "value", classOf[Array[Byte]])
      val instance = new PrivateStringHash(stringValue)
      instance.hash("t")
      Right(instance)
    catch case NonFatal(cause) => Left(unavailable(cause))

  private inline val RequiredFlag =
    "--add-opens=java.base/java.lang=ALL-UNNAMED"

  private val message: String =
    s"Hash.strings.privateJDK requires private JDK access. Start the JVM with $RequiredFlag."

  private def unavailable(cause: Throwable): IllegalStateException =
    new IllegalStateException(message, cause)

  final private class PrivateStringHash(stringValue: VarHandle) extends Hash[String]:
    override def hash(from: String): Long =
      val value = stringValue.get(from).asInstanceOf[Array[Byte]]
      MurmurHash3.murmurhash3_x64_64(value, 0, value.length, 0)
