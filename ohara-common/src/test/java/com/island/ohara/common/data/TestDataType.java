package com.island.ohara.common.data;

import com.island.ohara.common.rule.SmallTest;
import java.sql.Time;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class TestDataType extends SmallTest {

  @Test
  public void testAll() {
    Assert.assertEquals(DataType.all.size(), DataType.values().length);
  }

  @Test
  public void noDuplicateOrder() {
    Assert.assertEquals(
        DataType.all.size(),
        DataType.all.stream().map(t -> t.order).collect(Collectors.toSet()).size());
  }

  @Test
  public void noDuplicateName() {
    Assert.assertEquals(
        DataType.all.size(),
        DataType.all.stream().map(t -> t.name).collect(Collectors.toSet()).size());
  }

  @Test
  public void noDuplicateAlias() {
    Assert.assertEquals(
        DataType.all.size(),
        DataType.all.stream().map(t -> t.alias).collect(Collectors.toSet()).size());
  }

  @Test
  public void testOrder() {
    DataType.all.forEach(t -> Assert.assertEquals(t, DataType.of(t.order)));
  }

  @Test
  public void testName() {
    DataType.all.forEach(t -> Assert.assertEquals(t, DataType.of(t.name)));
  }

  @Test
  public void testAlias() {
    DataType.all.forEach(t -> Assert.assertEquals(t, DataType.of(t.alias)));
  }

  @Test
  public void testOfType() {
    Assert.assertEquals(DataType.BOOLEAN, DataType.from(false));
    Assert.assertEquals(DataType.SHORT, DataType.from((short) 1));
    Assert.assertEquals(DataType.INT, DataType.from(1));
    Assert.assertEquals(DataType.LONG, DataType.from((long) 1));
    Assert.assertEquals(DataType.FLOAT, DataType.from((float) 1));
    Assert.assertEquals(DataType.DOUBLE, DataType.from((double) 1));
    Assert.assertEquals(DataType.STRING, DataType.from("asd"));
    Assert.assertEquals(DataType.BYTE, DataType.from((byte) 1));
    Assert.assertEquals(DataType.BYTES, DataType.from(new byte[2]));
    Assert.assertEquals(DataType.ROW, DataType.from(Row.of(Cell.of("aa", "aa"))));
    Assert.assertEquals(DataType.OBJECT, DataType.from(new Time(123123)));
  }
}
