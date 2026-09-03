package se.thanh.pds.bloomfilter

/**
 * The implementation is based on https://github.com/alexandrnikitin/bloom-filter-scala
 * Copyright 2026 Alex Nikitin
 * Licensed under the MIT License.
 */

import se.thanh.pds.bloomfilter.internal.hashing.{
  MurmurHash3,
  PrivateStringHash,
  SafeStringHash,
  UnsafeStringHash
}

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

    given stringHash: Hash[String] = strings.default

  object strings:

    /**
     * Selects the first available allocation-free String hash in this order:
     * [[unsafe]], [[privateJDK]], then [[safe]].
     */
    val default: Hash[String] =
      UnsafeStringHash.instance
        .orElse(PrivateStringHash.instance)
        .getOrElse(SafeStringHash.instance)

    /**
     * Safe String hashing over logical UTF-16 code units.
     *
     * This implementation uses only public Java APIs and produces stable hashes
     * independently of the JVM's compact String representation.
     */
    lazy val safe: Hash[String] = SafeStringHash.instance

    /**
     * String hashing over sun.misc.Unsafe.
     *
     * On JDK 24 and later, `--sun-misc-unsafe-memory-access=allow` suppresses
     * warnings for this access, while `deny` makes this instance unavailable.
     * Its hashes depend on the JVM's String representation, so a filter must
     * use the same instance and JVM configuration for all reads and writes.
     */
    lazy val unsafe: Hash[String] =
      UnsafeStringHash.instance.fold(cause => throw cause, identity)

    /**
     * String hashing over the JDK's private backing byte array.
     *
     * Selecting this instance requires
     * `--add-opens=java.base/java.lang=ALL-UNNAMED`. Its hashes depend on the
     * JVM's String representation, so a filter must use the same instance and
     * JVM configuration for all reads and writes.
     */
    lazy val privateJDK: Hash[String] =
      PrivateStringHash.instance.fold(cause => throw cause, identity)
