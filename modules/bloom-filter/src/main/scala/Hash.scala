package se.thanh.pds.bloomfilter

/**
 * The implementation is based on https://github.com/alexandrnikitin/bloom-filter-scala
 * Copyright 2026 Alex Nikitin
 * Licensed under the MIT License.
 */

import se.thanh.pds.bloomfilter.internal.hashing.MurmurHash3

import java.lang.invoke.{ MethodHandles, VarHandle }

trait Hash[A]:
  def hash(from: A): Long

object Hash extends Hash.DefaultInstances:

  def apply[A](using h: Hash[A]): Hash[A] = h

  private[bloomfilter] trait DefaultInstances:
    given longHash: Hash[Long] with
      override def hash(from: Long): Long = MurmurHash3.fmix64(from)

    given byteArrayHash: Hash[Array[Byte]] with
      override def hash(from: Array[Byte]): Long =
        MurmurHash3.murmurhash3_x64_64(from, 0, from.length, 0)

    given stringHash: Hash[String] with
      override def hash(from: String): Long =
        val value = stringValue.get(from).asInstanceOf[Array[Byte]]
        MurmurHash3.murmurhash3_x64_64(value, 0, value.length, 0)

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
