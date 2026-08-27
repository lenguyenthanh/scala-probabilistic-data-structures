package se.thanh.pds.bloomfilter

object BenchmarkBloomFilter:

  def foreignMemory[T: Hash](numberOfItems: Long, falsePositiveRate: Double): OffHeapBloomFilter[T] =
    require(Runtime.version().feature() >= 22, "foreign memory benchmarks require JDK 22 or newer")
    offHeap(numberOfItems, falsePositiveRate)(new internal.ForeignMemoryBitArray(_))

  def unsafe[T: Hash](numberOfItems: Long, falsePositiveRate: Double): OffHeapBloomFilter[T] =
    offHeap(numberOfItems, falsePositiveRate)(new internal.UnsafeBitArray(_))

  private def offHeap[T: Hash](
      numberOfItems: Long,
      falsePositiveRate: Double
  )(createBits: Long => OffHeapBitArray): OffHeapBloomFilter[T] =
    val numberOfBits   = BloomFilter.optimalNumberOfBits(numberOfItems, falsePositiveRate)
    val numberOfHashes = BloomFilter.optimalNumberOfHashes(numberOfItems, numberOfBits)
    val bits           = createBits(numberOfBits)

    new BloomFilterImpl[T](numberOfBits, numberOfHashes, bits) with OffHeapBloomFilter[T]:
      override def close(): Unit = bits.close()
