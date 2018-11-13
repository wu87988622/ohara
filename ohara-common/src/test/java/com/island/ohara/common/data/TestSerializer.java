package com.island.ohara.common.data;

import com.island.ohara.common.rule.SmallTest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class TestSerializer extends SmallTest {

  @Test
  public void testBoolean() {
    Assert.assertTrue(Serializer.BOOLEAN.from(Serializer.BOOLEAN.to(true)));
    Assert.assertFalse(Serializer.BOOLEAN.from(Serializer.BOOLEAN.to(false)));
  }

  @Test
  public void testShort() {
    List<Short> data =
        Arrays.asList(Short.MIN_VALUE, (short) -10, (short) 0, (short) 10, Short.MAX_VALUE);
    data.forEach(
        v -> Assert.assertEquals((short) v, (short) Serializer.SHORT.from(Serializer.SHORT.to(v))));
  }

  @Test
  public void testInt() {
    List<Integer> data = Arrays.asList(Integer.MIN_VALUE, -10, 0, 10, Integer.MAX_VALUE);
    data.forEach(
        v -> Assert.assertEquals((int) v, (int) Serializer.INT.from(Serializer.INT.to(v))));
  }

  @Test
  public void testLong() {
    List<Long> data =
        Arrays.asList(Long.MIN_VALUE, (long) -10, (long) 0, (long) 10, Long.MAX_VALUE);
    data.forEach(
        v -> Assert.assertEquals((long) v, (long) Serializer.LONG.from(Serializer.LONG.to(v))));
  }

  @Test
  public void testFloat() {
    List<Float> data =
        Arrays.asList(Float.MIN_VALUE, (float) -10, (float) 0, (float) 10, Float.MAX_VALUE);
    data.forEach(v -> Assert.assertEquals(v, Serializer.FLOAT.from(Serializer.FLOAT.to(v)), 0.0));
  }

  @Test
  public void testDouble() {
    List<Double> data =
        Arrays.asList(Double.MIN_VALUE, (double) -10, (double) 0, (double) 10, Double.MAX_VALUE);
    data.forEach(v -> Assert.assertEquals(v, Serializer.DOUBLE.from(Serializer.DOUBLE.to(v)), 0.0));
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
    data.forEach(v -> Assert.assertEquals(v, Serializer.STRING.from(Serializer.STRING.to(v))));
  }

  @Test
  public void testRow() {
    List<Row> data =
        Arrays.asList(
            Row.of(Cell.of("abc", "aaa")),
            Row.of(Cell.of("abc", "aaa"), Cell.of("AA", "aaa")),
            Row.of(Collections.singletonList("tag"), Cell.of("abc", 123)),
            Row.of(Arrays.asList("a", "b"), Cell.of("abc", "aaa"), Cell.of("tt", "aaa")),
            Row.of(Cell.of("abc", Row.of(Cell.of("abc", "aaa")))));
    data.forEach(v -> Assert.assertEquals(v, Serializer.ROW.from(Serializer.ROW.to(v))));
  }
}
