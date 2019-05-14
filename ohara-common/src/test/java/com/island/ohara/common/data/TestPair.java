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
import java.util.*;
import org.junit.Assert;
import org.junit.Test;

public class TestPair extends SmallTest {

  @Test(expected = NullPointerException.class)
  public void testNULLElementsShouldThrowException() {
    Pair.of("bar", null);
    Pair.of(null, "foo");
    Pair.of(null, null);
  }

  @Test
  public void testEquals() {
    Pair<String, String> pair1 = Pair.of("left", "right");
    Pair<String, String> pair2 = Pair.of("left", "right");
    Assert.assertEquals(pair1, pair1);
    Assert.assertEquals(pair1, pair2);
    Assert.assertEquals(pair2, pair1);
  }

  @Test
  public void testEqualsOfDifferentType() {
    Pair<String, String> pair1 = Pair.of("left", "10");
    Pair<String, Integer> pair2 = Pair.of("left", 10);

    // different type can not be equal
    Assert.assertNotEquals(pair1, pair2);
  }

  @Test
  public void testHashCode() {
    Pair<String, String> pair1 = Pair.of("bar", "foo");
    Pair<String, String> pair2 = Pair.of("foo", "bar");

    Assert.assertNotEquals(pair1.hashCode(), pair2.hashCode());
  }

  @Test
  public void testGenericType() {
    List<String> list = Arrays.asList("1", "2");
    Map<String, String> map = new HashMap<>();
    map.put("key", "value");

    Pair<List<String>, Map<String, String>> pair = Pair.of(list, map);

    Assert.assertSame(pair.left(), list);
    Assert.assertSame(pair.right(), map);
  }

  @Test
  public void testToString() {
    Pair<String, String> pair = Pair.of("bar", "foo");

    Assert.assertTrue(pair.toString().contains("bar"));
    Assert.assertTrue(pair.toString().contains("foo"));
  }
}
