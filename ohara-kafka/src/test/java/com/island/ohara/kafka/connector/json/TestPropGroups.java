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

package com.island.ohara.kafka.connector.json;

import com.island.ohara.common.rule.SmallTest;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class TestPropGroups extends SmallTest {
  @Test
  public void testToPropGroups() {
    List<PropGroup> propGroups =
        PropGroups.ofJson(
            "["
                + "{"
                + "\"order\": 1,"
                + "\"props\": {"
                + "\"aa\": \"cc\","
                + "\"aa2\": \"cc2\""
                + "}"
                + "}"
                + "]");
    Assert.assertEquals(1, propGroups.size());
    Assert.assertEquals(1, propGroups.get(0).order());
    Assert.assertEquals(2, propGroups.get(0).size());
    Assert.assertEquals("cc", propGroups.get(0).props().get("aa"));
    Assert.assertEquals("cc2", propGroups.get(0).props().get("aa2"));
  }

  @Test
  public void testToStringToPropGroups() {
    List<PropGroup> propGroups =
        PropGroups.ofJson(
            "["
                + "{"
                + "\"order\": 1,"
                + "\"props\": {"
                + "\"aa\": \"cc\","
                + "\"aaa\": \"cc\""
                + "}"
                + "}"
                + "]");
    List<PropGroup> another = PropGroups.ofJson(PropGroups.toString(propGroups));
    Assert.assertEquals(propGroups.size(), another.size());
    Assert.assertEquals(propGroups.get(0).order(), another.get(0).order());
    Assert.assertEquals(propGroups.get(0).props(), another.get(0).props());
  }

  @Test
  public void testNullJson() {
    Assert.assertTrue(PropGroups.ofJson(null).isEmpty());
  }

  @Test
  public void testEmptyJson() {
    Assert.assertTrue(PropGroups.ofJson("").isEmpty());
  }

  @Test
  public void testNullStringJson() {
    Assert.assertTrue(PropGroups.ofJson("NULL").isEmpty());
  }

  @Test
  public void testNullStringJson2() {
    Assert.assertTrue(PropGroups.ofJson("null").isEmpty());
  }
}
