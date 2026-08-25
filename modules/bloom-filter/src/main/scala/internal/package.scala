package se.thanh.pds.bloomfilter.internal

extension (bitIndex: Long)

  private[internal] inline def wordByteOffset: Long =
    (bitIndex >>> 6) * java.lang.Long.BYTES

  private[internal] inline def bitMask: Long =
    1L << bitIndex

end extension
