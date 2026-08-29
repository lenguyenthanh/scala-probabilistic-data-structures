# Scala probabilistic data structures

This Scala 3 library provides mutable probabilistic data structures with a focus on predictable
memory use and performance. It currently includes on-heap and off-heap Bloom filters.

## Requirements

- Java 18 or newer
- Scala 3

The built-in hashing implementation requires access to `java.lang.String` internals. Start the JVM
with:

```text
--add-opens=java.base/java.lang=ALL-UNNAMED
```

For a forked sbt application, for example:

```scala
Compile / run / fork := true
Compile / run / javaOptions += "--add-opens=java.base/java.lang=ALL-UNNAMED"
```

## Installation

Only available from [JitPack](https://jitpack.io/#lenguyenthanh/scala-probabilistic-data-structures) **for now**.
Replace `<tag-or-commit>` with a Git tag or commit hash:

```scala
resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies +=
  "com.github.lenguyenthanh.scala-probabilistic-data-structures" %%
    "bloom-filter" % "<tag-or-commit>"
```

## Quickstart

`BloomFilter` sizes itself from the expected number of inserted items and the desired false-positive
rate. The filter may report a false positive, but it does not report false negatives for items that
have been added.

```scala mdoc
import se.thanh.pds.bloomfilter.BloomFilter

val filter = BloomFilter[Long](numberOfItems = 1_000L, falsePositiveRate = 0.01)

filter.add(17L)
filter.contains(17L)
filter.contains(16L)
filter.falsePositiveRate()
```

Built-in `Hash` instances are provided for `Long`, `String`, and `Array[Byte]`.

The default implementation is backed by an `Array[Long]`, so it's total bit size is limited
by nature of JVM Array size limit. So if you want a really really big total bit size, you'll
need off-heap variant.

@:callout(warning)
Off-heap BloomFilter use `Unsafe` or `Foreign Memory` to allocate memory off-heap (hence the name),
there fore you have to trigger the release of memory manually by callling `close()` after use.
@:@

```scala mdoc
import se.thanh.pds.bloomfilter.BloomFilter

val offHeap = BloomFilter.offHeap[Long](1_000L, 0.01)
offHeap.add(17L)
offHeap.contains(17L)
offHeap.contains(16L)
offHeap.falsePositiveRate()
offHeap.close() // we need to release memory for off-heap BloomFilter
```

@:callout(info)
On Java 22 and newer the off-heap implementation uses the Foreign Function and Memory API. Earlier
supported Java versions use an `Unsafe`-based implementation.
@:@

## Benchmarks

Latest benchmarks (29/08/2026) on an isolated hardware Intel(R) Core(TM) i7-7700 CPU @ 3.60GHz with JDK 25.
This is 2/3 times faster the original across implementations.


```
[info] StringItemBenchmark.ffmOffHeapAdd        latin1      1024        10  avgt   45  152.815 ± 0.015  ns/op
[info] StringItemBenchmark.ffmOffHeapAdd         utf16      1024        10  avgt   45  277.790 ± 0.134  ns/op
[info] StringItemBenchmark.ffmOffHeapGet        latin1      1024        10  avgt   45  153.407 ± 0.960  ns/op
[info] StringItemBenchmark.ffmOffHeapGet         utf16      1024        10  avgt   45  276.826 ± 0.055  ns/op
[info] StringItemBenchmark.originalAdd          latin1      1024        10  avgt   45  524.968 ± 0.452  ns/op
[info] StringItemBenchmark.originalAdd           utf16      1024        10  avgt   45  523.987 ± 0.471  ns/op
[info] StringItemBenchmark.originalGet          latin1      1024        10  avgt   45  523.736 ± 0.228  ns/op
[info] StringItemBenchmark.originalGet           utf16      1024        10  avgt   45  522.841 ± 0.238  ns/op
[info] StringItemBenchmark.onHeapAdd            latin1      1024        10  avgt   45  151.768 ± 0.039  ns/op
[info] StringItemBenchmark.onHeapAdd             utf16      1024        10  avgt   45  277.623 ± 0.072  ns/op
[info] StringItemBenchmark.onHeapGet            latin1      1024        10  avgt   45  150.851 ± 0.026  ns/op
[info] StringItemBenchmark.onHeapGet             utf16      1024        10  avgt   45  276.940 ± 0.031  ns/op
[info] StringItemBenchmark.unsafeOffHeapAdd     latin1      1024        10  avgt   45  152.085 ± 0.939  ns/op
[info] StringItemBenchmark.unsafeOffHeapAdd      utf16      1024        10  avgt   45  275.908 ± 0.166  ns/op
[info] StringItemBenchmark.unsafeOffHeapGet     latin1      1024        10  avgt   45  151.257 ± 0.646  ns/op
[info] StringItemBenchmark.unsafeOffHeapGet      utf16      1024        10  avgt   45  274.358 ± 0.085  ns/op
```
