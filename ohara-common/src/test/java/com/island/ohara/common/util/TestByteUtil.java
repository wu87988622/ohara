package com.island.ohara.common.util;

import com.island.ohara.common.rule.SmallTest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;

public class TestByteUtil extends SmallTest {

  @Test
  public void testBoolean() {
    Assert.assertTrue(ByteUtil.toBoolean(ByteUtil.toBytes(true)));
    Assert.assertFalse(ByteUtil.toBoolean(ByteUtil.toBytes(false)));
  }

  @Test
  public void testShort() {
    List<Short> data =
        Arrays.asList(Short.MIN_VALUE, (short) -10, (short) 0, (short) 10, Short.MAX_VALUE);
    data.forEach(v -> Assert.assertEquals((short) v, ByteUtil.toShort(ByteUtil.toBytes(v))));
  }

  @Test
  public void testInt() {
    List<Integer> data = Arrays.asList(Integer.MIN_VALUE, -10, 0, 10, Integer.MAX_VALUE);
    data.forEach(v -> Assert.assertEquals((int) v, ByteUtil.toInt(ByteUtil.toBytes(v))));
  }

  @Test
  public void testLong() {
    List<Long> data =
        Arrays.asList(Long.MIN_VALUE, (long) -10, (long) 0, (long) 10, Long.MAX_VALUE);
    data.forEach(v -> Assert.assertEquals((long) v, ByteUtil.toLong(ByteUtil.toBytes(v))));
  }

  @Test
  public void testFloat() {
    List<Float> data =
        Arrays.asList(Float.MIN_VALUE, (float) -10, (float) 0, (float) 10, Float.MAX_VALUE);
    data.forEach(v -> Assert.assertEquals(v, ByteUtil.toFloat(ByteUtil.toBytes(v)), 0.0));
  }

  @Test
  public void testDouble() {
    List<Double> data =
        Arrays.asList(Double.MIN_VALUE, (double) -10, (double) 0, (double) 10, Double.MAX_VALUE);
    data.forEach(v -> Assert.assertEquals(v, ByteUtil.toDouble(ByteUtil.toBytes(v)), 0.0));
  }

  @Test
  public void testString() {
    List<String> data =
        Arrays.asList(
            String.valueOf(Double.MIN_VALUE),
            "abc",
            "aaaaa",
            "Ccccc",
            String.valueOf(Double.MAX_VALUE));
    data.forEach(v -> Assert.assertEquals(v, ByteUtil.toString(ByteUtil.toBytes(v))));
  }

  @Test
  public void testBooleanComparator() {
    List<byte[]> data = Arrays.asList(ByteUtil.toBytes(true), ByteUtil.toBytes(false));
    data.forEach(v -> Assert.assertEquals(0, ByteUtil.BYTES_COMPARATOR.compare(v, v)));
    data.forEach(v -> Assert.assertEquals(0, ByteUtil.compare(v, v)));
  }

  @Test
  public void testShortComparator() {
    List<byte[]> data =
        Stream.of(Short.MIN_VALUE, (short) -10, (short) 0, (short) 10, Short.MAX_VALUE)
            .map(ByteUtil::toBytes)
            .collect(Collectors.toList());
    data.forEach(v -> Assert.assertEquals(0, ByteUtil.BYTES_COMPARATOR.compare(v, v)));
    data.forEach(v -> Assert.assertEquals(0, ByteUtil.compare(v, v)));

    short lhs = ByteUtil.toShort(ByteUtil.toBytes((short) -10));
    short rhs = ByteUtil.toShort(ByteUtil.toBytes((short) 20));
    Assert.assertTrue(lhs < rhs);
  }

  @Test
  public void testIntComparator() {
    List<byte[]> data =
        Stream.of(Integer.MIN_VALUE, -10, 0, 10, Integer.MAX_VALUE)
            .map(ByteUtil::toBytes)
            .collect(Collectors.toList());
    data.forEach(v -> Assert.assertEquals(0, ByteUtil.BYTES_COMPARATOR.compare(v, v)));
    data.forEach(v -> Assert.assertEquals(0, ByteUtil.compare(v, v)));

    int lhs = ByteUtil.toInt(ByteUtil.toBytes(-10));
    int rhs = ByteUtil.toInt(ByteUtil.toBytes(20));
    Assert.assertTrue(lhs < rhs);
  }

  @Test
  public void testLongComparator() {
    List<byte[]> data =
        Stream.of(Long.MIN_VALUE, (long) -10, (long) 0, (long) 10, Long.MAX_VALUE)
            .map(ByteUtil::toBytes)
            .collect(Collectors.toList());
    data.forEach(v -> Assert.assertEquals(0, ByteUtil.BYTES_COMPARATOR.compare(v, v)));
    data.forEach(v -> Assert.assertEquals(0, ByteUtil.compare(v, v)));

    long lhs = ByteUtil.toLong(ByteUtil.toBytes((long) -10));
    long rhs = ByteUtil.toLong(ByteUtil.toBytes((long) 20));
    Assert.assertTrue(lhs < rhs);
  }

  @Test
  public void testFloatComparator() {
    List<byte[]> data =
        Stream.of(Float.MIN_VALUE, (float) -10, (float) 0, (float) 10, Float.MAX_VALUE)
            .map(ByteUtil::toBytes)
            .collect(Collectors.toList());
    data.forEach(v -> Assert.assertEquals(0, ByteUtil.BYTES_COMPARATOR.compare(v, v)));
    data.forEach(v -> Assert.assertEquals(0, ByteUtil.compare(v, v)));

    float lhs = ByteUtil.toFloat(ByteUtil.toBytes((float) -10));
    float rhs = ByteUtil.toFloat(ByteUtil.toBytes((float) 20));
    Assert.assertTrue(lhs < rhs);
  }

  @Test
  public void testDoubleComparator() {
    List<byte[]> data =
        Stream.of(Double.MIN_VALUE, (double) -10, (double) 0, (double) 10, Double.MAX_VALUE)
            .map(ByteUtil::toBytes)
            .collect(Collectors.toList());
    data.forEach(v -> Assert.assertEquals(0, ByteUtil.BYTES_COMPARATOR.compare(v, v)));
    data.forEach(v -> Assert.assertEquals(0, ByteUtil.compare(v, v)));

    double lhs = ByteUtil.toDouble(ByteUtil.toBytes((double) -10));
    double rhs = ByteUtil.toDouble(ByteUtil.toBytes((double) 20));
    Assert.assertTrue(lhs < rhs);
  }

  @Test
  public void testStringComparator() {
    List<byte[]> data =
        Stream.of(
                String.valueOf(Double.MIN_VALUE),
                "abc",
                "aaaaa",
                "Ccccc",
                String.valueOf(Double.MAX_VALUE))
            .map(ByteUtil::toBytes)
            .collect(Collectors.toList());
    data.forEach(v -> Assert.assertEquals(0, ByteUtil.BYTES_COMPARATOR.compare(v, v)));
    data.forEach(v -> Assert.assertEquals(0, ByteUtil.compare(v, v)));

    byte[] lhs = ByteUtil.toBytes("abc");
    byte[] rhs = ByteUtil.toBytes("bc");
    Assert.assertTrue(ByteUtil.compare(lhs, rhs) < 0);
    Assert.assertTrue(ByteUtil.BYTES_COMPARATOR.compare(lhs, rhs) < 0);
  }
}
