package se.thanh.pds.bloomfilter.internal.hashing

import se.thanh.pds.bloomfilter.Hash

private[bloomfilter] object UnsafeStringHash:

  import sun.misc.Unsafe
  import scala.annotation.nowarn
  import scala.util.control.NonFatal

  @nowarn("cat=deprecation")
  def instance: Either[Throwable, Hash[String]] =
    try
      classOf[Unsafe].getDeclaredFields
        .find(_.getType == classOf[Unsafe])
        .map: field =>
          field.setAccessible(true)
          field.get(null).asInstanceOf[Unsafe]
        .toRight(new IllegalStateException(message))
        .map: unsafe =>
          val stringValueOffset = unsafe.objectFieldOffset(classOf[String].getDeclaredField("value"))
          val instance          = new UnsafeStringHash(unsafe, stringValueOffset)
          // Test whether the runtime allows private access with Unsafe.
          instance.hash("t")
          instance
    catch case e @ NonFatal(_) => Left(unavailable(e))

  private def unavailable(cause: Throwable): IllegalStateException =
    new IllegalStateException(message, cause)

  private val message: String =
    "Hash.strings.unsafe is unavailable on this JVM. " +
      "On supported JDKs, ensure sun.misc.Unsafe memory access is allowed."

  @nowarn("cat=deprecation")
  final private class UnsafeStringHash(unsafe: Unsafe, stringValueOffset: Long) extends Hash[String]:

    override def hash(from: String): Long =
      val value = unsafe.getObject(from, stringValueOffset).asInstanceOf[Array[Byte]]
      MurmurHash3.murmurhash3_x64_64(value, 0, value.length, 0)
