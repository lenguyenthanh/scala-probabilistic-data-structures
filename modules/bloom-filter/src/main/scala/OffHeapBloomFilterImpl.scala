package se.thanh.pds.bloomfilter

final private class OffHeapBloomFilterImpl[T](numberOfHashes: Int, bits: OffHeapBitArray)(using Hash[T])
    extends BloomFilterImpl[T](numberOfHashes, bits),
      OffHeapBloomFilter[T]:

  override def close(): Unit = bits.close()
