package se.thanh.pds.bloomfilter.hashmatrix;

public final class HashDenyOpenedSpec extends HashMatrixSuite {
  public HashDenyOpenedSpec() {
    super(UnsafeMode.DENY, true);
  }
}
