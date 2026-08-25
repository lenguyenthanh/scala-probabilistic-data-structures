package se.thanh.pds.bloomfilter.laws

import org.scalacheck.{ Gen, Prop }
import org.typelevel.discipline.Laws
import se.thanh.pds.bloomfilter.BloomFilter

final class BloomFilterTests[T] private (
    create: (Long, Double) => BloomFilter[T],
    elementGen: Gen[T]
) extends Laws:

  private val laws = new BloomFilterLaws(create)

  // Keep allocations representative but bounded: the largest generated filter is about 120 KiB.
  private val inputGen: Gen[(Long, List[T])] =
    for
      expectedItems <- Gen.chooseNum[Long](1, 100000)
      items         <- Gen.nonEmptyListOf(elementGen)
    yield (expectedItems, items)

  def bloomFilter: RuleSet =
    new DefaultRuleSet(
      name = "bloomFilter",
      parent = None,
      "a fresh filter reports definite absence" ->
        Prop.forAll(inputGen)(laws.freshFilterReportsAbsence),
      "added items might be present"      -> Prop.forAll(inputGen)(laws.addedItemsMightBePresent),
      "repeated adds preserve membership" -> Prop.forAll(inputGen)(laws.repeatedAddsPreserveMembership)
    )

object BloomFilterTests:
  def apply[T](
      create: (Long, Double) => BloomFilter[T],
      elementGen: Gen[T]
  ): BloomFilterTests[T] = new BloomFilterTests(create, elementGen)
