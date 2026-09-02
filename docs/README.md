# Scala probabilistic data structures

This Scala 3 library provides mutable probabilistic data structures with a focus on predictable
memory use and performance. It currently includes on-heap and off-heap Bloom filters.

## Requirements

- Java 18 or newer
- Scala 3

Default use on Java 18 and newer requires no JVM flags.

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

## String hashing

The default `Hash[String]` selects the first available allocation-free implementation in this order:

1. `sun.misc.Unsafe`
2. private-JDK `VarHandle`
3. safe `String.charAt`

The selection happens once, when the default String hasher is first used. `Unsafe` and `VarHandle`
hash the JVM's compact String backing array, while the safe implementation hashes logical UTF-16
code units in little-endian byte order. Applications that persist filters or share them between JVMs
with different configurations should explicitly select one implementation for all reads and writes.

Use `Hash.safe` to pin the public-API-only implementation:

```scala
import se.thanh.pds.bloomfilter.{ BloomFilter, Hash }
import Hash.safe.given

val filter = BloomFilter[String](numberOfItems = 1_000L, falsePositiveRate = 0.01)
```

Use `Hash.privateJDK` to pin the `VarHandle` implementation:

```scala
import se.thanh.pds.bloomfilter.{ BloomFilter, Hash }
import Hash.privateJDK.given

val filter = BloomFilter[String](numberOfItems = 1_000L, falsePositiveRate = 0.01)
```

@:callout(warning)
`Hash.privateJDK` requires this exact JVM option and fails during selection if access is not
available:

```text
--add-opens=java.base/java.lang=ALL-UNNAMED
```

@:@

Use `Hash.unsafe` to pin the `sun.misc.Unsafe` implementation. On JDK 24 and later, Unsafe memory
access produces a warning by default. `--sun-misc-unsafe-memory-access=allow` suppresses the warning;
`--sun-misc-unsafe-memory-access=deny` disables this implementation and makes the default continue
to the `VarHandle` fallback.

## Off heap BloomFilter

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
[info] Benchmark                             (charset)  (length)  (tokens)  Mode  Cnt    Score   Error  Units
[info] StringItemBenchmark.ffmOffHeapAdd        latin1        32        10  avgt   45   46.159 ± 0.014  ns/op
[info] StringItemBenchmark.ffmOffHeapAdd        latin1      1024        10  avgt   45  508.509 ± 0.158  ns/op
[info] StringItemBenchmark.ffmOffHeapAdd         utf16        32        10  avgt   45   49.032 ± 0.015  ns/op
[info] StringItemBenchmark.ffmOffHeapAdd         utf16      1024        10  avgt   45  504.808 ± 0.210  ns/op
[info] StringItemBenchmark.ffmOffHeapGet        latin1        32        10  avgt   45   46.549 ± 0.009  ns/op
[info] StringItemBenchmark.ffmOffHeapGet        latin1      1024        10  avgt   45  510.505 ± 0.577  ns/op
[info] StringItemBenchmark.ffmOffHeapGet         utf16        32        10  avgt   45   49.297 ± 0.062  ns/op
[info] StringItemBenchmark.ffmOffHeapGet         utf16      1024        10  avgt   45  505.077 ± 0.302  ns/op
[info] StringItemBenchmark.onHeapAdd            latin1        32        10  avgt   45   44.630 ± 0.015  ns/op
[info] StringItemBenchmark.onHeapAdd            latin1      1024        10  avgt   45  509.140 ± 0.812  ns/op
[info] StringItemBenchmark.onHeapAdd             utf16        32        10  avgt   45   47.288 ± 0.034  ns/op
[info] StringItemBenchmark.onHeapAdd             utf16      1024        10  avgt   45  502.994 ± 0.432  ns/op
[info] StringItemBenchmark.onHeapGet            latin1        32        10  avgt   45   44.896 ± 0.008  ns/op
[info] StringItemBenchmark.onHeapGet            latin1      1024        10  avgt   45  508.376 ± 0.635  ns/op
[info] StringItemBenchmark.onHeapGet             utf16        32        10  avgt   45   47.494 ± 0.058  ns/op
[info] StringItemBenchmark.onHeapGet             utf16      1024        10  avgt   45  501.552 ± 1.961  ns/op
[info] StringItemBenchmark.originalAdd          latin1        32        10  avgt   45   97.925 ± 0.194  ns/op
[info] StringItemBenchmark.originalAdd          latin1      1024        10  avgt   45  524.476 ± 0.615  ns/op
[info] StringItemBenchmark.originalAdd           utf16        32        10  avgt   45   97.916 ± 0.132  ns/op
[info] StringItemBenchmark.originalAdd           utf16      1024        10  avgt   45  524.090 ± 0.602  ns/op
[info] StringItemBenchmark.originalGet          latin1        32        10  avgt   45   96.408 ± 0.063  ns/op
[info] StringItemBenchmark.originalGet          latin1      1024        10  avgt   45  523.925 ± 1.485  ns/op
[info] StringItemBenchmark.originalGet           utf16        32        10  avgt   45   96.317 ± 0.115  ns/op
[info] StringItemBenchmark.originalGet           utf16      1024        10  avgt   45  522.558 ± 0.448  ns/op
[info] StringItemBenchmark.unsafeOffHeapAdd     latin1        32        10  avgt   45   44.499 ± 0.007  ns/op
[info] StringItemBenchmark.unsafeOffHeapAdd     latin1      1024        10  avgt   45  506.656 ± 0.170  ns/op
[info] StringItemBenchmark.unsafeOffHeapAdd      utf16        32        10  avgt   45   47.241 ± 0.009  ns/op
[info] StringItemBenchmark.unsafeOffHeapAdd      utf16      1024        10  avgt   45  502.724 ± 0.093  ns/op
[info] StringItemBenchmark.unsafeOffHeapGet     latin1        32        10  avgt   45   43.844 ± 0.013  ns/op
[info] StringItemBenchmark.unsafeOffHeapGet     latin1      1024        10  avgt   45  507.192 ± 0.644  ns/op
[info] StringItemBenchmark.unsafeOffHeapGet      utf16        32        10  avgt   45   46.559 ± 0.006  ns/op
[info] StringItemBenchmark.unsafeOffHeapGet      utf16      1024        10  avgt   45  501.779 ± 0.128  ns/op
```
