package se.thanh.pds.bloomfilter.internal.hashing

import se.thanh.pds.bloomfilter.Hash

private[bloomfilter] object SafeStringHash:

  val instance: Hash[String] = new Hash[String]:
    override def hash(from: String): Long =
      MurmurHash3.murmurhash3_x64_64_utf16le(from, 0)
