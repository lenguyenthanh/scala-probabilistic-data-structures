package se.thanh.pds.bloomfilter.internal.hashing

import se.thanh.pds.bloomfilter.Hash

object UnsafeStringHash:

  import sun.misc.Unsafe
  import scala.annotation.nowarn
  import scala.util.control.NonFatal

  def instance: Hash[String] =
    try initialize()
    catch
      case cause: LinkageError => throw unavailable(cause)
      case NonFatal(cause)     => throw unavailable(cause)

  private[bloomfilter] def tryInstance: Option[Hash[String]] =
    try Some(instance)
    catch
      case _: LinkageError => None
      case NonFatal(_)     => None

  @nowarn("cat=deprecation")
  private def initialize(): Hash[String] =
    val unsafe = classOf[Unsafe].getDeclaredFields
      .find(_.getType == classOf[Unsafe])
      .map: field =>
        field.setAccessible(true)
        field.get(null).asInstanceOf[Unsafe]
      .getOrElse(throw new IllegalStateException("cannot find sun.misc.Unsafe instance"))
    val stringValueOffset = unsafe.objectFieldOffset(classOf[String].getDeclaredField("value"))
    val instance          = new UnsafeStringHash(unsafe, stringValueOffset)
    instance.hash("")
    instance

  private def unavailable(cause: Throwable): IllegalStateException =
    new IllegalStateException(
      "Hash.unsafe is unavailable on this JVM. " +
        "On supported JDKs, ensure sun.misc.Unsafe memory access is allowed.",
      cause
    )

  @nowarn("cat=deprecation")
  final private class UnsafeStringHash(unsafe: Unsafe, stringValueOffset: Long) extends Hash[String]:

    override def hash(from: String): Long =
      val value = unsafe.getObject(from, stringValueOffset).asInstanceOf[Array[Byte]]
      MurmurHash3.murmurhash3_x64_64(value, 0, value.length, 0)
