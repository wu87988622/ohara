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

package com.island.ohara.common.setting;

import com.island.ohara.common.data.Column;
import com.island.ohara.common.data.DataType;
import com.island.ohara.common.rule.OharaTest;
import com.island.ohara.common.util.CommonUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class TestPropGroup extends OharaTest {
  @Test
  public void testToPropGroup() {
    PropGroup propGroup =
        PropGroup.ofJson(
            "[" + "{" + "\"order\": 1," + "\"aa\": \"cc\"," + "\"aa2\": \"cc2\"" + "}" + "]");
    Assert.assertEquals(1, propGroup.size());
    Assert.assertEquals("1", propGroup.props(0).get("order"));
    Assert.assertEquals("cc", propGroup.props(0).get("aa"));
    Assert.assertEquals("cc2", propGroup.props(0).get("aa2"));
  }

  @Test
  public void testToStringToPropGroup() {
    PropGroup propGroup =
        PropGroup.ofJson(
            "[" + "{" + "\"order\": 1," + "\"aa\": \"cc\"," + "\"aaa\": \"cc\"" + "}" + "]");
    PropGroup another = PropGroup.ofJson(propGroup.toJsonString());
    Assert.assertEquals(propGroup.size(), another.size());
    Assert.assertEquals(propGroup.props(0).get("order"), another.props(0).get("order"));
    Assert.assertEquals(propGroup.props(0).get("aa"), another.props(0).get("aa"));
    Assert.assertEquals(propGroup.props(0).get("aaa"), another.props(0).get("aaa"));
  }

  @Test(expected = NullPointerException.class)
  public void testNullJson() {
    Assert.assertTrue(PropGroup.ofJson(null).isEmpty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyJson() {
    Assert.assertTrue(PropGroup.ofJson("").isEmpty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullStringJson() {
    Assert.assertTrue(PropGroup.ofJson("NULL").isEmpty());
  }

  @Test(expected = NullPointerException.class)
  public void testNullStringJson2() {
    Assert.assertTrue(PropGroup.ofJson("null").isEmpty());
  }

  @Test
  public void testEmpty() {
    Assert.assertTrue(PropGroup.of(Collections.emptyList()).isEmpty());
  }

  @Test
  public void testEmpty2() {
    Assert.assertTrue(PropGroup.of(Collections.singletonList(Collections.emptyMap())).isEmpty());
  }

  @Test
  public void testEmpty3() {
    Assert.assertEquals(
        0, PropGroup.of(Collections.singletonList(Collections.emptyMap())).numberOfElements());
  }

  @Test
  public void testJson() {
    String json = "[" + "{" + "\"order\": 1," + "\"aa\": \"cc\"," + "\"aaa\": \"ccc\"" + "}" + "]";

    PropGroup propGroup = PropGroup.ofJson(json);
    Assert.assertEquals(1, propGroup.size());
    Assert.assertEquals("1", propGroup.iterator().next().get("order"));
    Assert.assertEquals("cc", propGroup.iterator().next().get("aa"));
    Assert.assertEquals("ccc", propGroup.iterator().next().get("aaa"));
  }

  @Test
  public void testColumns() {
    List<Column> columns =
        Arrays.asList(
            Column.builder()
                .name(CommonUtils.randomString())
                .newName(CommonUtils.randomString())
                .dataType(DataType.STRING)
                .order(5)
                .build(),
            Column.builder()
                .name(CommonUtils.randomString())
                .newName(CommonUtils.randomString())
                .dataType(DataType.STRING)
                .order(1)
                .build());
    PropGroup pgs = PropGroup.ofColumns(columns);
    List<Column> another = pgs.toColumns();
    Assert.assertEquals(columns.size(), another.size());
    another.forEach(c -> Assert.assertTrue(columns.contains(c)));
  }

  @Test
  public void parseJson() {
    String json =
        "["
            + "{"
            + "\"order\": 1,"
            + "\"name\": \"cc\","
            + "\"newName\": \"ccc\","
            + "\"dataType\": \"BYTES\""
            + "}"
            + "]";
    PropGroup pgs = PropGroup.ofJson(json);
    List<Column> columns = pgs.toColumns();
    Assert.assertEquals(1, columns.size());
    Assert.assertEquals(1, columns.get(0).order());
    Assert.assertEquals("cc", columns.get(0).name());
    Assert.assertEquals("ccc", columns.get(0).newName());
    Assert.assertEquals(DataType.BYTES, columns.get(0).dataType());

    String json2 =
        "[" + "{" + "\"order\": 1," + "\"name\": \"cc\"," + "\"dataType\": \"BYTES\"" + "}" + "]";

    PropGroup pgs2 = PropGroup.ofJson(json2);
    List<Column> columns2 = pgs2.toColumns();
    Assert.assertEquals(1, columns2.size());
    Assert.assertEquals(1, columns2.get(0).order());
    Assert.assertEquals("cc", columns2.get(0).name());
    Assert.assertEquals("cc", columns2.get(0).newName());
    Assert.assertEquals(DataType.BYTES, columns2.get(0).dataType());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testRemove() {
    PropGroup pgs = PropGroup.of(Collections.singletonList(Collections.singletonMap("a", "b")));
    pgs.iterator().remove();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testRemoveFromList() {
    PropGroup pgs = PropGroup.of(Collections.singletonList(Collections.singletonMap("a", "b")));
    pgs.raw().remove(0);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testRemoveFromMap() {
    PropGroup pgs = PropGroup.of(Collections.singletonList(Collections.singletonMap("a", "b")));
    pgs.raw().get(0).remove("a");
  }

  @Test
  public void testConvert() {
    PropGroup propGroup =
        PropGroup.of(Collections.singletonList(Collections.singletonMap("a", "b")));
    PropGroup another = PropGroup.ofJson(propGroup.toJsonString());
    Assert.assertEquals(propGroup, another);
  }

  @Test
  public void testToColumnWithLowerCase() {
    Column column =
        Column.builder()
            .name(CommonUtils.randomString(10))
            .dataType(DataType.BOOLEAN)
            .order(3)
            .build();
    Map<String, String> raw = new HashMap<>(PropGroup.toPropGroup(column));
    raw.put(
        SettingDef.COLUMN_DATA_TYPE_KEY, raw.get(SettingDef.COLUMN_DATA_TYPE_KEY).toLowerCase());
    PropGroup group = PropGroup.of(Collections.singletonList(raw));
    Column another = group.toColumns().get(0);
    Assert.assertEquals(column, another);
  }
}
