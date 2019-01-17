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
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public class TestRow extends SmallTest {

  @Test
  public void testEmpty() {
    Assert.assertEquals(Row.EMPTY, Row.EMPTY);
  }

  @Test
  public void testEquals() {
    Row row = Row.of(Arrays.asList("tag", "tag2"), Cell.of("aa", "aa"), Cell.of("b", 123));
    Row row2 = Row.of(Arrays.asList("tag", "tag2"), Cell.of("aa", "aa"), Cell.of("b", 123));
    Assert.assertEquals(row, row);
    Assert.assertEquals(row, row2);
    Assert.assertEquals(row2, row);
  }

  @Test
  public void testEqualsWithoutTags() {
    Row row = Row.of(Arrays.asList("tag", "tag2"), Cell.of("aa", "aa"), Cell.of("b", 123));
    Row row2 = Row.of(Collections.singletonList("tag"), Cell.of("aa", "aa"), Cell.of("b", 123));
    Assert.assertTrue(row.equals(row, false));
    Assert.assertTrue(row.equals(row2, false));
    Assert.assertTrue(row2.equals(row, false));
  }

  @Test
  public void testCells() {
    Row row = Row.of(Arrays.asList("tag", "tag2"), Cell.of("aa", "aa"), Cell.of("b", 123));
    Assert.assertEquals(2, row.size());
    Assert.assertEquals(2, row.cells().size());
    Assert.assertEquals(Cell.of("aa", "aa"), row.cell(0));
    Assert.assertEquals(Cell.of("aa", "aa"), row.cell("aa"));
  }

  @Test
  public void testTags() {
    Row row = Row.of(Arrays.asList("tag", "tag2"), Cell.of("aa", "aa"), Cell.of("b", 123));
    Assert.assertEquals(2, row.tags().size());
    Assert.assertEquals("tag", row.tags().get(0));
    Assert.assertEquals("tag2", row.tags().get(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void duplicateNameIsIllegal() {
    Row.of(Arrays.asList("tag", "tag2"), Cell.of("aa", "aa"), Cell.of("aa", 123));
  }

  @Test
  public void composeRow() {
    Row row = Row.of(Cell.of("abc", Row.of(Cell.of("abc", "aaa"))));
    Row row2 = Row.of(Cell.of("abc", Row.of(Cell.of("abc", "aaa"))));
    Assert.assertEquals(row, row);
    Assert.assertEquals(row, row2);
    Assert.assertEquals(row2, row);
  }
}
