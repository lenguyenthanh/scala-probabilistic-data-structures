package se.thanh.pds.bloomfilter.internal.hashing

import se.thanh.pds.bloomfilter.Hash

object UnsafeStringHash:

  import sun.misc.Unsafe
  import scala.annotation.nowarn
  import scala.util.Try

  def instance: Hash[String] = new UnsafeStringHash

  private val unsafe: Unsafe = Try {
    classOf[Unsafe].getDeclaredFields
      .find(_.getType == classOf[Unsafe])
      .map: field =>
        field.setAccessible(true)
        field.get(null).asInstanceOf[Unsafe]
      .getOrElse(throw new IllegalStateException("cannot find sun.misc.Unsafe instance"))
  }.recover { case cause: Throwable =>
    throw new ExceptionInInitializerError(cause)
  }.get

  @nowarn("cat=deprecation")
  private val stringValueOffset: Long =
    unsafe.objectFieldOffset(classOf[String].getDeclaredField("value"))

  private inline def stringValue(from: String): Array[Byte] =
    unsafe.getObject(from, stringValueOffset).asInstanceOf[Array[Byte]]

  final private class UnsafeStringHash extends Hash[String]:

    override def hash(from: String): Long =
      val value = stringValue(from)
      MurmurHash3.murmurhash3_x64_64(value, 0, value.length, 0)
