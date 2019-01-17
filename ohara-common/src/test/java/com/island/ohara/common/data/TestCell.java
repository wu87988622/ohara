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
import org.junit.Assert;
import org.junit.Test;

public class TestCell extends SmallTest {

  @Test
  public void testEquals() {
    Cell cell = Cell.of("abc", "abc");
    Assert.assertEquals(cell, cell);
    Assert.assertEquals(cell, Cell.of("abc", "abc"));
    Assert.assertEquals(Cell.of("abc", "abc"), cell);

    Cell cell2 = Cell.of("abc", 123);
    Assert.assertEquals(cell2, cell2);
    Assert.assertNotEquals(cell, cell2);
    Assert.assertNotEquals(cell2, cell);

    Cell cell3 = Cell.of("abc", "Adasd".getBytes());
    Assert.assertEquals(cell3, cell3);
    Assert.assertEquals(cell3, Cell.of("abc", "Adasd".getBytes()));
    Assert.assertEquals(Cell.of("abc", "Adasd".getBytes()), cell3);
  }

  @Test(expected = NullPointerException.class)
  public void testNullName() {
    Cell.of(null, "abc");
  }

  @Test(expected = NullPointerException.class)
  public void testNullValue() {
    Cell.of("abc", null);
  }

  @Test
  public void testHashCode() {
    Cell cell = Cell.of("abc", "abc");
    Assert.assertEquals(cell.hashCode(), cell.hashCode());
    Assert.assertEquals(cell.hashCode(), Cell.of("abc", "abc").hashCode());

    Cell cell2 = Cell.of("abc", "abc".getBytes());
    Assert.assertEquals(cell2.hashCode(), cell2.hashCode());
    Assert.assertEquals(cell2.hashCode(), Cell.of("abc", "abc".getBytes()).hashCode());
  }

  @Test
  public void cellComposeRow() {
    Cell c = Cell.of("abc", Row.of(Cell.of("abc", "aaa")));
    Assert.assertEquals(c.name(), "abc");
    Assert.assertEquals(c.value(), Row.of(Cell.of("abc", "aaa")));
  }
}
