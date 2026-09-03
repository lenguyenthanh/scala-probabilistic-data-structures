package se.thanh.pds.bloomfilter.hashmatrix;

public final class HashAllowOpenedSpec extends HashMatrixSuite {
  public HashAllowOpenedSpec() {
    super(UnsafeMode.ALLOW, true);
  }
}
