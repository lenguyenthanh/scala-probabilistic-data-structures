package se.thanh.pds.bloomfilter.laws

import se.thanh.pds.bloomfilter.BloomFilter

final class BloomFilterLaws[T](create: (Long, Double) => BloomFilter[T]):

  private def withFilter[A](expectedItems: Long)(run: BloomFilter[T] => A): A =
    val filter = create(expectedItems, 0.01)
    try run(filter)
    finally
      filter match
        case closeable: AutoCloseable => closeable.close()
        case _                        => ()

  def freshFilterReportsAbsence(expectedItems: Long, items: List[T]): Boolean =
    withFilter(expectedItems): filter =>
      items.forall(item => !filter.contains(item))

  def addedItemsMightBePresent(expectedItems: Long, items: List[T]): Boolean =
    withFilter(expectedItems): filter =>
      items.foreach(filter.add)
      items.forall(filter.contains)

  def repeatedAddsPreserveMembership(expectedItems: Long, items: List[T]): Boolean =
    withFilter(expectedItems): filter =>
      items.foreach(filter.add)
      items.foreach(filter.add)
      items.forall(filter.contains)
