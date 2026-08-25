package se.thanh.pds.bloomfilter

import org.scalacheck.Prop.forAll
import org.scalacheck.{ Arbitrary, Gen }

import java.nio.charset.StandardCharsets

class BloomFilterRegressionSpec extends munit.ScalaCheckSuite:

  property("a non-Latin-1 string does not collide with the Latin-1 string of its leading bytes"):
    val collisionPairGen =
      Gen
        .nonEmptyListOf(Gen.oneOf('日', '本', '語', 'あ', 'い', 'Ω', 'П'))
        .map: chars =>
          val wide = chars.mkString
          // The UTF-16LE encoding is exactly the compact-string backing array for this content.
          val leadingBytes = wide.getBytes(StandardCharsets.UTF_16LE).take(wide.length)
          (wide, new String(leadingBytes.map(byte => (byte & 0xff).toChar)))

    forAll(collisionPairGen): (wide, latin1Twin) =>
      val filter = BloomFilter[String](100000L, 0.01)
      filter.add(wide)
      !filter.mightContain(latin1Twin)

  property("an item added to one filter is not required to be in another"):
    // Guards against the degenerate implementation where `mightContain` always returns true.
    forAll(Gen.listOfN(100, Arbitrary.arbitrary[Long])): (items: List[Long]) =>
      val filter          = BloomFilter[Long](100000L, 0.01)
      val (added, absent) = items.splitAt(50)
      added.foreach(filter.add)
      val absentOnly = absent.filterNot(added.contains)
      absentOnly.count(filter.mightContain) < absentOnly.size

  property("optimalNumberOfBits grows with a stricter false positive rate"):
    forAll(Gen.chooseNum[Long](1, 1000000)): (numberOfItems: Long) =>
      BloomFilter.optimalNumberOfBits(numberOfItems, 0.001) >
        BloomFilter.optimalNumberOfBits(numberOfItems, 0.01)
