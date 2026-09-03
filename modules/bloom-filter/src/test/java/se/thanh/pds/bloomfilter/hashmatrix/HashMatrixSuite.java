package se.thanh.pds.bloomfilter.hashmatrix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import se.thanh.pds.bloomfilter.Hash;
import se.thanh.pds.bloomfilter.Hash$;
import se.thanh.pds.bloomfilter.internal.hashing.DefaultStringHash$;
import se.thanh.pds.bloomfilter.internal.hashing.MurmurHash3$;

public abstract class HashMatrixSuite {
  protected enum UnsafeMode {
    DEFAULT("default", null),
    ALLOW("allow", "--sun-misc-unsafe-memory-access=allow"),
    WARN("warn", "--sun-misc-unsafe-memory-access=warn"),
    DEBUG("debug", "--sun-misc-unsafe-memory-access=debug"),
    DENY("deny", "--sun-misc-unsafe-memory-access=deny");

    private final String name;
    private final String argument;

    UnsafeMode(String name, String argument) {
      this.name = name;
      this.argument = argument;
    }
  }

  private enum HashTarget {
    DEFAULT("Hash[String]"),
    UNSAFE("Hash.unsafe"),
    PRIVATE_JDK("Hash.privateJDK"),
    SAFE("Hash.safe");

    private final String label;

    HashTarget(String label) {
      this.label = label;
    }

    @SuppressWarnings("unchecked")
    Hash<String> acquire() {
      switch (this) {
        case DEFAULT:
          return (Hash<String>) Hash$.MODULE$.stringHash();
        case UNSAFE:
          return Hash.unsafe$.MODULE$.stringHash();
        case PRIVATE_JDK:
          return Hash.privateJDK$.MODULE$.stringHash();
        case SAFE:
          return Hash.safe$.MODULE$.stringHash();
        default:
          throw new AssertionError("unknown hash target: " + this);
      }
    }
  }

  private enum HashSemantics {
    COMPACT,
    LOGICAL_UTF16_LE
  }

  private static final String ADD_OPENS = "--add-opens=java.base/java.lang=ALL-UNNAMED";
  private static final List<String> VALUES =
      List.of(
          "hello world",
          "h\u00e9llo \u00ff",
          "a\u0000b",
          "\u65e5\u672c\u8a9e",
          "x\ud83d\ude00y",
          "\ud800",
          "\udc00");

  private final UnsafeMode expectedMode;
  private final boolean expectedAddOpens;

  protected HashMatrixSuite(UnsafeMode expectedMode, boolean expectedAddOpens) {
    this.expectedMode = expectedMode;
    this.expectedAddOpens = expectedAddOpens;
  }

  @Test
  public final void receivesExactlyTheExpectedMatrixJvmArguments() {
    assertEquals(expectedMode.name, System.getProperty("hash.matrix.unsafe-mode"));
    assertEquals(Boolean.toString(expectedAddOpens), System.getProperty("hash.matrix.add-opens"));

    List<String> unsafeArguments = new ArrayList<>();
    List<String> addOpensArguments = new ArrayList<>();
    for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
      if (argument.startsWith("--sun-misc-unsafe-memory-access=")) {
        unsafeArguments.add(argument);
      }
      if (argument.startsWith("--add-opens=java.base/java.lang=")) {
        addOpensArguments.add(argument);
      }
    }

    assertEquals(
        expectedMode.argument == null ? List.of() : List.of(expectedMode.argument), unsafeArguments);
    assertEquals(expectedAddOpens ? List.of(ADD_OPENS) : List.of(), addOpensArguments);
  }

  @Test
  public final void acquiresAndHashesWithDefault() {
    verify(HashTarget.DEFAULT);
  }

  @Test
  public final void acquiresAndHashesWithUnsafe() {
    verify(HashTarget.UNSAFE);
  }

  @Test
  public final void acquiresAndHashesWithPrivateJdk() {
    verify(HashTarget.PRIVATE_JDK);
  }

  @Test
  public final void acquiresAndHashesWithSafe() {
    verify(HashTarget.SAFE);
  }

  private void verify(HashTarget target) {
    boolean expectedAvailable;
    switch (target) {
      case DEFAULT:
      case SAFE:
        expectedAvailable = true;
        break;
      case UNSAFE:
        expectedAvailable = expectedMode != UnsafeMode.DENY;
        break;
      case PRIVATE_JDK:
        expectedAvailable = expectedAddOpens;
        break;
      default:
        throw new AssertionError("unknown hash target: " + target);
    }

    try {
      Hash<String> hash = target.acquire();
      assertTrue(target.label + " unexpectedly initialized", expectedAvailable);
      verifyImplementation(target);
      verifyHashing(target, hash);
    } catch (LinkageError | RuntimeException cause) {
      if (expectedAvailable) {
        throw cause;
      }
      String expectedMessage;
      switch (target) {
        case UNSAFE:
          expectedMessage = "Hash.unsafe is unavailable";
          break;
        case PRIVATE_JDK:
          expectedMessage = "Hash.privateJDK requires private JDK access";
          break;
        default:
          throw new AssertionError(target.label + " unexpectedly failed", cause);
      }
      assertTrue(failureChain(cause), causeChainContains(cause, expectedMessage));
    }
  }

  private void verifyImplementation(HashTarget target) {
    if (target == HashTarget.DEFAULT) {
      String expected;
      if (expectedMode != UnsafeMode.DENY) {
        expected = "Unsafe";
      } else if (expectedAddOpens) {
        expected = "VarHandle";
      } else {
        expected = "Safe";
      }
      assertEquals(expected, DefaultStringHash$.MODULE$.implementation().toString());
    }
  }

  private void verifyHashing(HashTarget target, Hash<String> hash) {
    HashSemantics expectedSemantics;
    if (target == HashTarget.SAFE
        || (target == HashTarget.DEFAULT
            && expectedMode == UnsafeMode.DENY
            && !expectedAddOpens)) {
      expectedSemantics = HashSemantics.LOGICAL_UTF16_LE;
    } else {
      expectedSemantics = HashSemantics.COMPACT;
    }

    List<Long> observed = new ArrayList<>();
    for (String value : VALUES) {
      long actual = hash.hash(value);
      assertEquals(value, expectedHash(value, expectedSemantics), actual);
      observed.add(actual);
    }

    int repetitions = expectedMode == UnsafeMode.DEBUG ? 1 : 100;
    for (int repetition = 0; repetition < repetitions; repetition++) {
      for (int index = 0; index < VALUES.size(); index++) {
        assertEquals(VALUES.get(index), observed.get(index).longValue(), hash.hash(VALUES.get(index)));
      }
    }
  }

  private static long expectedHash(String value, HashSemantics semantics) {
    switch (semantics) {
      case COMPACT:
        return hashBytes(compactBytes(value));
      case LOGICAL_UTF16_LE:
        return hashBytes(utf16LeBytes(value));
      default:
        throw new AssertionError("unknown hash semantics: " + semantics);
    }
  }

  private static boolean causeChainContains(Throwable cause, String expectedMessage) {
    for (Throwable current = cause; current != null; current = current.getCause()) {
      if (current.getMessage() != null && current.getMessage().contains(expectedMessage)) {
        return true;
      }
    }
    return false;
  }

  private static String failureChain(Throwable cause) {
    StringBuilder result = new StringBuilder();
    for (Throwable current = cause; current != null; current = current.getCause()) {
      if (result.length() > 0) {
        result.append(" -> ");
      }
      result.append(current.getClass().getName());
      if (current.getMessage() != null) {
        result.append(": ").append(current.getMessage());
      }
    }
    return result.toString();
  }

  private static long hashBytes(byte[] bytes) {
    return MurmurHash3$.MODULE$.murmurhash3_x64_64(bytes, 0, bytes.length, 0);
  }

  private static byte[] compactBytes(String value) {
    boolean latin1 = true;
    for (int index = 0; index < value.length(); index++) {
      if (value.charAt(index) > 0xff) {
        latin1 = false;
        break;
      }
    }
    if (!latin1) {
      return utf16LeBytes(value);
    }

    byte[] bytes = new byte[value.length()];
    for (int index = 0; index < value.length(); index++) {
      bytes[index] = (byte) value.charAt(index);
    }
    return bytes;
  }

  private static byte[] utf16LeBytes(String value) {
    byte[] bytes = new byte[value.length() * 2];
    for (int index = 0; index < value.length(); index++) {
      char codeUnit = value.charAt(index);
      bytes[index * 2] = (byte) codeUnit;
      bytes[index * 2 + 1] = (byte) (codeUnit >>> 8);
    }
    return bytes;
  }
}
