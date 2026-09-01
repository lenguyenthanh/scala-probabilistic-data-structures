package se.thanh.pds.bloomfilter

/**
 * The implementation is based on https://github.com/alexandrnikitin/bloom-filter-scala
 * Copyright 2026 Alex Nikitin
 * Licensed under the MIT License.
 */

import se.thanh.pds.bloomfilter.internal.hashing.{ MurmurHash3, UnsafeStringHash }

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
        MurmurHash3.murmurhash3_x64_64_utf16le(from, 0)

  /**
   * Opt-in String hashing over the JDK's private backing byte array.
   *
   * Selecting this instance requires
   * `--add-opens=java.base/java.lang=ALL-UNNAMED`. Its hashes depend on the
   * JVM's String representation, so a filter must use the same instance and
   * JVM configuration for all reads and writes.
   */
  object UnsafeCompact:
    given stringHash: Hash[String] = UnsafeStringHash.instance
