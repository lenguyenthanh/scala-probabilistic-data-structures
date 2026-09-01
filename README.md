> [!IMPORTANT]
> This started as Scala 3 port of [original
> Originally a port of [original  project](https://github.com/alexandrnikitin/bloom-filter-scala),
> this library remains heavily inspired by it while introducing bug fixes, improvements and additional functionality.

# Scala probabilistic data structures

A Scala 3 library of efficient probabilistic data structures. The first available data structure
is a mutable Bloom filter with on-heap and off-heap storage.

See the [documentation](https://lenguyenthanh.github.io/scala-probabilistic-data-structures/)
for installation and a quickstart.

The default `Hash[String]` is allocation-free and requires no JVM access flags on Java 18+. An
explicit `Hash.UnsafeCompact` instance is available for long Latin-1-heavy workloads, but it requires
`--add-opens=java.base/java.lang=ALL-UNNAMED` and hashes the JVM's private String backing bytes. Its
results depend on the JVM's compact-string configuration and layout, so a filter must use the same
hasher and JVM configuration for every write and query.
