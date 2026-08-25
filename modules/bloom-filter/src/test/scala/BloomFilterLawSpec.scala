package se.thanh.pds.bloomfilter

import org.scalacheck.{ Arbitrary, Gen }

import laws.BloomFilterTests

class BloomFilterLawSpec extends munit.DisciplineSuite:

  private def onHeap[T: Hash]: (Long, Double) => BloomFilter[T] =
    (expectedItems, falsePositiveRate) => BloomFilter[T](expectedItems, falsePositiveRate)

  private def offHeap[T: Hash]: (Long, Double) => BloomFilter[T] =
    (expectedItems, falsePositiveRate) => BloomFilter.offHeap[T](expectedItems, falsePositiveRate)

  private val nonLatin1StringGen: Gen[String] =
    Gen.nonEmptyListOf(Gen.oneOf('日', '本', '語', 'あ', 'い', 'Ω', 'μ', 'П', 'р')).map(_.mkString)

  checkAll(
    "BloomFilter.onHeap[Long]",
    BloomFilterTests(onHeap[Long], Arbitrary.arbitrary[Long]).bloomFilter
  )
  checkAll(
    "BloomFilter.onHeap[String]",
    BloomFilterTests(onHeap[String], Arbitrary.arbitrary[String]).bloomFilter
  )
  checkAll(
    "BloomFilter.onHeap[non-Latin-1 String]",
    BloomFilterTests(onHeap[String], nonLatin1StringGen).bloomFilter
  )
  checkAll(
    "BloomFilter.onHeap[Array[Byte]]",
    BloomFilterTests(onHeap[Array[Byte]], Arbitrary.arbitrary[Array[Byte]]).bloomFilter
  )

  checkAll(
    "BloomFilter.offHeap[Long]",
    BloomFilterTests(offHeap[Long], Arbitrary.arbitrary[Long]).bloomFilter
  )
  checkAll(
    "BloomFilter.offHeap[String]",
    BloomFilterTests(offHeap[String], Arbitrary.arbitrary[String]).bloomFilter
  )
  checkAll(
    "BloomFilter.offHeap[non-Latin-1 String]",
    BloomFilterTests(offHeap[String], nonLatin1StringGen).bloomFilter
  )
  checkAll(
    "BloomFilter.offHeap[Array[Byte]]",
    BloomFilterTests(offHeap[Array[Byte]], Arbitrary.arbitrary[Array[Byte]]).bloomFilter
  )
