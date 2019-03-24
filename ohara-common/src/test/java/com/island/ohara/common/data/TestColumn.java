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

package com.island.ohara.common.data;

import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestColumn extends SmallTest {

  @Test(expected = NullPointerException.class)
  public void testNullName() {
    Column.builder().name(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyName() {
    Column.builder().name("");
  }

  @Test(expected = NullPointerException.class)
  public void testNullNewName() {
    Column.builder().newName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyNewName() {
    Column.builder().newName("");
  }

  @Test(expected = NullPointerException.class)
  public void testNullDataType() {
    Column.builder().dataType(null);
  }

  @Test
  public void testEqual() {
    String name = CommonUtils.randomString(10);
    DataType type = DataType.BOOLEAN;
    int order = 100;
    Column column = Column.builder().name(name).dataType(type).order(order).build();
    Assert.assertEquals(name, column.name());
    Assert.assertEquals(type, column.dataType());
    Assert.assertEquals(order, column.order());
  }

  @Test
  public void testEqualWithNewName() {
    String name = CommonUtils.randomString(10);
    String newName = CommonUtils.randomString(10);
    DataType type = DataType.BOOLEAN;
    int order = 100;
    Column column =
        Column.builder().name(name).newName(newName).dataType(type).order(order).build();
    Assert.assertEquals(name, column.name());
    Assert.assertEquals(newName, column.newName());
    Assert.assertEquals(type, column.dataType());
    Assert.assertEquals(order, column.order());
  }
}
