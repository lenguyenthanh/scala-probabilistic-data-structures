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

The default `Hash[String]` applies MurmurHash3 with seed zero to the String's logical UTF-16 code
units in little-endian byte order. It is allocation-free, uses only public Java APIs, preserves lone
surrogate code units, and has the same result regardless of the JVM's compact-string mode or native
byte order.

Applications that prioritize hashing speed for long Latin-1 strings can explicitly select the
private-JDK implementation:

```scala
import se.thanh.pds.bloomfilter.{ BloomFilter, Hash }
import Hash.UnsafeCompact.given

val filter = BloomFilter[String](numberOfItems = 1_000L, falsePositiveRate = 0.01)
```

`Hash.UnsafeCompact` requires this exact JVM option and fails during selection if access is not
available:

```text
--add-opens=java.base/java.lang=ALL-UNNAMED
```

For a forked sbt application, for example:

```scala
Compile / run / fork := true
Compile / run / javaOptions += "--add-opens=java.base/java.lang=ALL-UNNAMED"
```

The unsafe instance hashes the private `String.value` byte array directly, preserving the previous
hasher's representation-dependent behavior. With compact strings enabled this normally means one
byte per Latin-1 code unit; disabling compact strings changes those hashes because the backing array
changes. JVM layout and byte-order differences can change hashes as well.

@:callout(warning)
Hasher selection and JVM String representation are part of a Bloom filter's data format. The default
and `UnsafeCompact` hashes can differ, so do not use one hasher to query an existing persisted or
off-heap filter populated with the other. Every reader and writer must select the same given, and
`UnsafeCompact` users must also keep a compatible JVM configuration.
@:@

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

The production String hash benchmark covers the `safe` and `unsafe` implementations with ASCII,
non-ASCII Latin-1, BMP, supplementary, early-wide, and late-wide inputs at 8, 32, 256, and 1024
UTF-16 code units. Run the matrix with GC profiling:

```text
sbt "benchmark/Jmh/run -prof gc se.thanh.pds.bloomfilter.benchmark.StringHashBenchmark.*"
```

Both paths are intended to allocate effectively zero bytes per hash after setup. The safe default is
the portable choice. `UnsafeCompact` is expected to have its clearest advantage on long ASCII and
Latin-1 strings because it hashes the backing bytes directly. Measure on the deployment JDK and CPU
before accepting private-JDK access and representation-dependent hashes as operational dependencies.
