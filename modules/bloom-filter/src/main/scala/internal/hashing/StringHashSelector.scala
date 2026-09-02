package se.thanh.pds.bloomfilter.internal.hashing

import se.thanh.pds.bloomfilter.Hash

private[bloomfilter] object SafeStringHash:

  val instance: Hash[String] = new Hash[String]:
    override def hash(from: String): Long =
      MurmurHash3.murmurhash3_x64_64_utf16le(from, 0)

private[bloomfilter] enum StringHashImplementation:
  case Unsafe, VarHandle, Safe

final private case class SelectedStringHash(
    implementation: StringHashImplementation,
    hash: Hash[String]
)

final private[bloomfilter] class StringHashSelector(
    unsafe: () => Option[Hash[String]],
    varHandle: () => Option[Hash[String]],
    safe: () => Hash[String]
) extends Hash[String]:

  private lazy val selected: SelectedStringHash =
    unsafe()
      .map(SelectedStringHash(StringHashImplementation.Unsafe, _))
      .orElse:
        varHandle().map(SelectedStringHash(StringHashImplementation.VarHandle, _))
      .getOrElse:
        SelectedStringHash(StringHashImplementation.Safe, safe())

  private[bloomfilter] def implementation: StringHashImplementation =
    selected.implementation

  override def hash(from: String): Long = selected.hash.hash(from)

private[bloomfilter] object DefaultStringHash:

  val instance: StringHashSelector = new StringHashSelector(
    () => UnsafeStringHash.tryInstance,
    () => PrivateStringHash.tryInstance,
    () => SafeStringHash.instance
  )

  private[bloomfilter] def implementation: StringHashImplementation =
    instance.implementation
