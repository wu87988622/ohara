/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package oharastream.ohara.common.data;

import java.sql.Time;
import java.util.stream.Collectors;
import oharastream.ohara.common.rule.OharaTest;
import org.junit.Assert;
import org.junit.Test;

public class TestDataType extends OharaTest {

  @Test
  public void testAll() {
    Assert.assertEquals(DataType.all.size(), DataType.values().length);
  }

  @Test
  public void noDuplicateOrder() {
    Assert.assertEquals(
        DataType.all.size(),
        DataType.all.stream().map(t -> t.order).collect(Collectors.toUnmodifiableSet()).size());
  }

  @Test
  public void noDuplicateName() {
    Assert.assertEquals(
        DataType.all.size(),
        DataType.all.stream()
            .map(Enum<DataType>::name)
            .collect(Collectors.toUnmodifiableSet())
            .size());
  }

  @Test
  public void testOrder() {
    DataType.all.forEach(t -> Assert.assertEquals(t, DataType.of(t.order)));
  }

  @Test
  public void testName() {
    DataType.all.forEach(t -> Assert.assertEquals(t, DataType.valueOf(t.name())));
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
