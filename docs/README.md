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

`BloomFilter` calculates optimal size and number of hashes by itself from the expected number of inserted
items and the desired false-positive rate. The filter may report a false positive, but it does not report
false negatives for items that have been added.

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
hash the JVM's private String backing array, while the safe implementation hashes logical UTF-16
code units in little-endian byte order. Applications that persist filters or share them between JVMs
with different configurations should explicitly select one implementation for all reads and writes.

Use `Hash.strings.safe` to pin the public-API-only implementation:

```scala
import se.thanh.pds.bloomfilter.{ BloomFilter, Hash }

val filter = BloomFilter[String](numberOfItems = 1_000L, falsePositiveRate = 0.01)(using
  Hash.strings.safe
)
```

Use `Hash.strings.privateJDK` to pin the `VarHandle` implementation:

```scala
import se.thanh.pds.bloomfilter.{ BloomFilter, Hash }

val filter = BloomFilter[String](numberOfItems = 1_000L, falsePositiveRate = 0.01)(using
  Hash.strings.privateJDK
)
```

@:callout(warning)
`Hash.strings.privateJDK` requires this exact JVM option and fails during selection if access is not
available:

```text
--add-opens=java.base/java.lang=ALL-UNNAMED
```

@:@

Use `Hash.strings.unsafe` to pin the `sun.misc.Unsafe` implementation. On JDK 24 and later, Unsafe
memory access produces a warning by default. `--sun-misc-unsafe-memory-access=allow` suppresses the
warning; `--sun-misc-unsafe-memory-access=deny` disables this implementation and makes the default
continue to the `VarHandle` fallback.

## Off heap BloomFilter

The default implementation is backed by an `Array[Long]`, so it's total bit size is limited
by nature of JVM Array size limit. So if you want a really really big total bit size, you'll
need off-heap variant.

@:callout(warning)
Off-heap BloomFilter use `Unsafe` or `Foreign Memory` to allocate memory off-heap (hence the name),
there fore you have to trigger the release of memory manually by callling `close()` after use.
@:@

@:callout(info)
On Java 22 and newer the off-heap implementation uses the Foreign Function and Memory API. Earlier
supported Java versions use an `Unsafe`-based implementation.
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

## Benchmarks

Latest benchmarks (06/09/2026) on an isolated hardware Intel(R) Core(TM) i7-7700 CPU @ 3.60GHz with JDK 25.

### `BloomFilter[String]` with defaut (`Unsafe`) string hash implementation

This is 2/3 times faster the original across implementations.

```
[info] Benchmark                             (charset)  (length)  (tokens)  Mode  Cnt    Score   Error  Units
[info] StringItemBenchmark.ffmOffHeapAdd        latin1        32        10  avgt   45   36.458 ± 0.009  ns/op
[info] StringItemBenchmark.ffmOffHeapAdd        latin1      1024        10  avgt   45  154.724 ± 1.067  ns/op
[info] StringItemBenchmark.ffmOffHeapAdd         utf16        32        10  avgt   45   40.268 ± 0.008  ns/op
[info] StringItemBenchmark.ffmOffHeapAdd         utf16      1024        10  avgt   45  278.674 ± 0.096  ns/op
[info] StringItemBenchmark.ffmOffHeapGet        latin1        32        10  avgt   45   35.620 ± 0.036  ns/op
[info] StringItemBenchmark.ffmOffHeapGet        latin1      1024        10  avgt   45  152.527 ± 0.015  ns/op
[info] StringItemBenchmark.ffmOffHeapGet         utf16        32        10  avgt   45   39.285 ± 0.052  ns/op
[info] StringItemBenchmark.ffmOffHeapGet         utf16      1024        10  avgt   45  277.341 ± 0.106  ns/op
[info] StringItemBenchmark.onHeapAdd            latin1        32        10  avgt   45   35.061 ± 0.076  ns/op
[info] StringItemBenchmark.onHeapAdd            latin1      1024        10  avgt   45  152.061 ± 0.052  ns/op
[info] StringItemBenchmark.onHeapAdd             utf16        32        10  avgt   45   38.951 ± 0.051  ns/op
[info] StringItemBenchmark.onHeapAdd             utf16      1024        10  avgt   45  277.843 ± 0.033  ns/op
[info] StringItemBenchmark.onHeapGet            latin1        32        10  avgt   45   34.623 ± 0.081  ns/op
[info] StringItemBenchmark.onHeapGet            latin1      1024        10  avgt   45  151.576 ± 0.052  ns/op
[info] StringItemBenchmark.onHeapGet             utf16        32        10  avgt   45   38.414 ± 0.010  ns/op
[info] StringItemBenchmark.onHeapGet             utf16      1024        10  avgt   45  277.411 ± 0.107  ns/op
[info] StringItemBenchmark.originalAdd          latin1        32        10  avgt   45   97.985 ± 0.128  ns/op
[info] StringItemBenchmark.originalAdd          latin1      1024        10  avgt   45  524.308 ± 0.636  ns/op
[info] StringItemBenchmark.originalAdd           utf16        32        10  avgt   45   97.912 ± 0.141  ns/op
[info] StringItemBenchmark.originalAdd           utf16      1024        10  avgt   45  524.752 ± 0.647  ns/op
[info] StringItemBenchmark.originalGet          latin1        32        10  avgt   45   96.307 ± 0.012  ns/op
[info] StringItemBenchmark.originalGet          latin1      1024        10  avgt   45  524.411 ± 1.445  ns/op
[info] StringItemBenchmark.originalGet           utf16        32        10  avgt   45   96.273 ± 0.011  ns/op
[info] StringItemBenchmark.originalGet           utf16      1024        10  avgt   45  524.160 ± 1.370  ns/op
[info] StringItemBenchmark.unsafeOffHeapAdd     latin1        32        10  avgt   45   34.311 ± 0.010  ns/op
[info] StringItemBenchmark.unsafeOffHeapAdd     latin1      1024        10  avgt   45  151.183 ± 0.019  ns/op
[info] StringItemBenchmark.unsafeOffHeapAdd      utf16        32        10  avgt   45   37.925 ± 0.011  ns/op
[info] StringItemBenchmark.unsafeOffHeapAdd      utf16      1024        10  avgt   45  276.174 ± 0.080  ns/op
[info] StringItemBenchmark.unsafeOffHeapGet     latin1        32        10  avgt   45   33.767 ± 0.008  ns/op
[info] StringItemBenchmark.unsafeOffHeapGet     latin1      1024        10  avgt   45  150.913 ± 0.032  ns/op
[info] StringItemBenchmark.unsafeOffHeapGet      utf16        32        10  avgt   45   37.416 ± 0.025  ns/op
[info] StringItemBenchmark.unsafeOffHeapGet      utf16      1024        10  avgt   45  275.230 ± 0.093  ns/op
```

### String hash implementations

We can see that `Unsafe` and `VarHandle` has similar performance and both are faster than `safe` variant.
The longer the string, the different performance is getting bigger.


```
[info] Benchmark                 (coder)  (implementation)  (length)  Mode  Cnt    Score   Error  Units
[info] StringHashBenchmark.hash    ascii            unsafe        32  avgt   45   13.729 ± 0.009  ns/op
[info] StringHashBenchmark.hash    ascii            unsafe      1024  avgt   45  130.115 ± 0.035  ns/op
[info] StringHashBenchmark.hash    ascii         varhandle        32  avgt   45   16.852 ± 0.006  ns/op
[info] StringHashBenchmark.hash    ascii         varhandle      1024  avgt   45  133.443 ± 0.050  ns/op
[info] StringHashBenchmark.hash    ascii              safe        32  avgt   45   20.825 ± 0.004  ns/op
[info] StringHashBenchmark.hash    ascii              safe      1024  avgt   45  487.173 ± 0.077  ns/op
[info] StringHashBenchmark.hash   latin1            unsafe        32  avgt   45   13.725 ± 0.007  ns/op
[info] StringHashBenchmark.hash   latin1            unsafe      1024  avgt   45  133.165 ± 1.118  ns/op
[info] StringHashBenchmark.hash   latin1         varhandle        32  avgt   45   16.856 ± 0.009  ns/op
[info] StringHashBenchmark.hash   latin1         varhandle      1024  avgt   45  133.452 ± 0.022  ns/op
[info] StringHashBenchmark.hash   latin1              safe        32  avgt   45   20.834 ± 0.007  ns/op
[info] StringHashBenchmark.hash   latin1              safe      1024  avgt   45  486.233 ± 0.723  ns/op
```
