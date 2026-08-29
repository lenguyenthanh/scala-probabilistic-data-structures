package se.thanh.pds.bloomfilter

class SampleUsageSpec extends munit.FunSuite:

  test("create, put and check"):
    // given Hash[String] with
    //   import scala.util.Random
    //   override def hash(from: String): Long = Random().nextLong()

    val bloomFilter = BloomFilter[String](1000, 0.01)

    bloomFilter.add("")
    bloomFilter.add("Hello!")
    bloomFilter.add("8f16c986824e40e7885a032ddd29a7d3")

    assert(bloomFilter.mightContain(""))
    assert(bloomFilter.mightContain("Hello!"))
    assert(bloomFilter.mightContain("8f16c986824e40e7885a032ddd29a7d3"))
