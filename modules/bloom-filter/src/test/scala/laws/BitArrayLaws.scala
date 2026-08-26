package se.thanh.pds.bloomfilter.laws

import se.thanh.pds.bloomfilter.BitArray

final class BitArrayLaws(create: Long => BitArray):

  private def withBits[A](size: Long)(run: BitArray => A): A =
    val bits = create(size)
    try run(bits)
    finally
      bits match
        case closeable: AutoCloseable => closeable.close()
        case _                        => ()

  def roundsSizeUpToWholeWords(minSize: Long): Boolean =
    withBits(minSize): bits =>
      bits.size >= minSize &&
        bits.size % java.lang.Long.SIZE == 0 &&
        bits.size - minSize < java.lang.Long.SIZE

  def startsEmpty(size: Long, indices: List[Long]): Boolean =
    withBits(size): bits =>
      bits.nonEmptyBits == 0L && indices.forall(index => !bits.get(index))

  def readsSetIndices(size: Long, indices: List[Long]): Boolean =
    withBits(size): bits =>
      indices.foreach(bits.set)
      indices.forall(bits.get)

  def settingOneIndexIsIsolated(size: Long, index: Long): Boolean =
    withBits(size): bits =>
      bits.set(index)
      (0L until size).forall(candidate => bits.get(candidate) == (candidate == index))

  def settingIndicesIsIdempotent(size: Long, indices: List[Long]): Boolean =
    withBits(size): once =>
      withBits(size): twice =>
        indices.foreach(once.set)
        indices.foreach(twice.set)
        indices.foreach(twice.set)
        once.nonEmptyBits == twice.nonEmptyBits &&
        (0L until size).forall(index => once.get(index) == twice.get(index))

  def countsDistinctSetIndices(size: Long, indices: List[Long]): Boolean =
    withBits(size): bits =>
      indices.foreach(bits.set)
      indices.foreach(bits.set)
      bits.nonEmptyBits == indices.distinct.size.toLong

  def addressesLastValidIndex(minSize: Long): Boolean =
    withBits(minSize): bits =>
      bits.set(bits.size - 1)
      bits.get(bits.size - 1) && bits.nonEmptyBits == 1L
