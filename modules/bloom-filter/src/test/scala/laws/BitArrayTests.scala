package se.thanh.pds.bloomfilter.laws

import org.scalacheck.{ Gen, Prop }
import org.typelevel.discipline.Laws
import se.thanh.pds.bloomfilter.BitArray

final class BitArrayTests private (create: Long => BitArray) extends Laws:

  private val laws = new BitArrayLaws(create)

  // Word boundaries concentrate the representation errors these laws are intended to expose.
  private val sizeGen: Gen[Long] =
    Gen.oneOf[Long](1, 63, 64, 65, 127, 128, 129, 1000, 100000)

  private val sizeAndIndicesGen: Gen[(Long, List[Long])] =
    for
      size    <- sizeGen
      indices <- Gen.listOf(Gen.chooseNum[Long](0, size - 1))
    yield (size, indices)

  private val sizeAndIndexGen: Gen[(Long, Long)] =
    for
      size  <- sizeGen
      index <- Gen.chooseNum[Long](0, size - 1)
    yield (size, index)

  def bitArray: RuleSet =
    new DefaultRuleSet(
      name = "bitArray",
      parent = None,
      "rounds the size up to whole words" -> Prop.forAll(sizeGen)(laws.roundsSizeUpToWholeWords),
      "starts empty"                      -> Prop.forAll(sizeAndIndicesGen)(laws.startsEmpty),
      "reads every set index"             -> Prop.forAll(sizeAndIndicesGen)(laws.readsSetIndices),
      "setting one index is isolated"     -> Prop.forAll(sizeAndIndexGen)(laws.settingOneIndexIsIsolated),
      "setting indices is idempotent"     -> Prop.forAll(sizeAndIndicesGen)(laws.settingIndicesIsIdempotent),
      "counts distinct set indices"       -> Prop.forAll(sizeAndIndicesGen)(laws.countsDistinctSetIndices),
      "addresses the last valid index"    -> Prop.forAll(sizeGen)(laws.addressesLastValidIndex)
    )

object BitArrayTests:
  def apply(create: Long => BitArray): BitArrayTests = new BitArrayTests(create)
