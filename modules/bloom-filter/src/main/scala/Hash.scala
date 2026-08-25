package se.thanh.pds.bloomfilter

import se.thanh.pds.bloomfilter.internal.hashing.MurmurHash3

import java.lang.invoke.{ MethodHandles, VarHandle }

/**
 * This is adapted from original version by Alex Nikitin in
 * https://github.com/alexandrnikitin/bloom-filter-scala.
 */
trait Hash[A]:
  def hash(from: A): Long

object Hash:

  def apply[A](using h: Hash[A]): Hash[A] = h

  given Hash[Long] with
    override def hash(from: Long): Long = MurmurHash3.fmix64(from)

  given Hash[Array[Byte]] with
    override def hash(from: Array[Byte]): Long =
      MurmurHash3.murmurhash3_x64_64(from, 0, from.length, 0)

  private val stringValue: VarHandle =
    try
      MethodHandles
        .privateLookupIn(classOf[String], MethodHandles.lookup())
        .findVarHandle(classOf[String], "value", classOf[Array[Byte]])
    catch
      case th: Throwable =>
        throw new ExceptionInInitializerError(
          new IllegalStateException(
            "Cannot access String.value, which bloomfilter needs to hash strings " +
              "without copying them. Run the JVM with --add-opens java.base/java.lang=ALL-UNNAMED.",
            th
          )
        )

  given Hash[String] with
    override def hash(from: String): Long =
      val value = stringValue.get(from).asInstanceOf[Array[Byte]]
      MurmurHash3.murmurhash3_x64_64(value, 0, value.length, 0)
