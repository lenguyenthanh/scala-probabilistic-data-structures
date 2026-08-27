package se.thanh.pds.bloomfilter

/**
 * This is adapted from original version by Alex Nikitin in
 * https://github.com/alexandrnikitin/bloom-filter-scala.
 */

private[bloomfilter] class BloomFilterImpl[T](
    val numberOfBits: Long,
    val numberOfHashes: Int,
    private val bits: BitArray
)(using hashFor: Hash[T])
    extends BloomFilter[T]:

  override def add(x: T): Unit =
    val hash  = hashFor.hash(x)
    val hash1 = hash >>> 32
    val hash2 = (hash << 32) >> 32

    var i = 0
    while i < numberOfHashes do
      bits.set(index(hash1, hash2, i))
      i += 1

  override def mightContain(x: T): Boolean =
    val hash  = hashFor.hash(x)
    val hash1 = hash >>> 32
    val hash2 = (hash << 32) >> 32

    var i      = 0
    var result = true
    while i < numberOfHashes && result do
      if !bits.get(index(hash1, hash2, i)) then result = false
      i += 1
    result

  def expectedFalsePositiveRate(): Double =
    java.lang.Math.pow(bits.nonEmptyBits.toDouble / numberOfBits, numberOfHashes.toDouble)

  private inline def index(hash1: Long, hash2: Long, iteration: Int): Long =
    ((hash1 + iteration * hash2) & Long.MaxValue) % numberOfBits
