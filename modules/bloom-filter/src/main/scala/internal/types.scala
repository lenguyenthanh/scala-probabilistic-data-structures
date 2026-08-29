package se.thanh.pds.bloomfilter
package internal

private[bloomfilter] object types:
  opaque type PositiveInt <: Int = Int
  object PositiveInt:
    def unsafe(value: Int, name: String): PositiveInt =
      require(value > 0, s"$name must be positive")
      value
    def unsafe(value: Int): PositiveInt =
      value

  opaque type PositiveLong <: Long = Long
  object PositiveLong:
    def unsafe(value: Long, name: String): PositiveLong =
      require(value > 0, s"$name must be positive")
      value

    def unsafe(value: Long): PositiveLong =
      value
