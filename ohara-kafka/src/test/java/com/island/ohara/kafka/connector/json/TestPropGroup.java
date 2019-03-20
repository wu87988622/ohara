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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.ohara.common.data.Column;
import com.island.ohara.common.data.DataType;
import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.junit.Test;

public class TestPropGroup extends SmallTest {
  @Test
  public void testEqual() throws IOException {
    PropGroup group = PropGroup.of(3, CommonUtils.randomString(5), CommonUtils.randomString(5));
    ObjectMapper mapper = new ObjectMapper();
    Assert.assertEquals(
        group,
        mapper.readValue(mapper.writeValueAsString(group), new TypeReference<PropGroup>() {}));
  }

  @Test
  public void testGetter() {
    int order = 100;
    String key = CommonUtils.randomString(5);
    String value = CommonUtils.randomString(5);
    PropGroup group = PropGroup.of(order, key, value);
    Assert.assertEquals(order, group.order());
    Assert.assertEquals(1, group.props().size());
    Assert.assertEquals(value, group.props().get(key));
  }

  @Test(expected = NullPointerException.class)
  public void nullKey() {
    PropGroup.of(1, null, "asd");
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyKey() {
    PropGroup.of(1, "", "asd");
  }

  @Test(expected = NullPointerException.class)
  public void nullProps() {
    PropGroup.of(1, null);
  }

  @Test
  public void emptyProps() {
    PropGroup.of(1, Collections.emptyMap());
  }

  @Test
  public void nullValueShouldDisappear() {
    PropGroup group = PropGroup.of(1, "Asdad", null);
    Assert.assertEquals(0, group.size());
  }

  @Test
  public void emptyValueShouldDisappear() {
    PropGroup group = PropGroup.of(1, "Asdad", "");
    Assert.assertEquals(0, group.size());
  }

  @Test
  public void testToString() {
    String name = CommonUtils.randomString(5);
    String value = CommonUtils.randomString(5);
    PropGroup group = PropGroup.of(10, name, value);
    Assert.assertTrue(group.toString().contains("10"));
    Assert.assertTrue(group.toString().contains(name));
    Assert.assertTrue(group.toString().contains(value));
  }

  @Test
  public void testSize() {
    int order = 1000;
    int numberOfProps = 10;
    PropGroup group =
        PropGroup.of(
            order,
            IntStream.range(0, numberOfProps)
                .mapToObj(Integer::toString)
                .collect(Collectors.toMap(i -> i, i -> i)));
    Assert.assertEquals(order, group.order());
    Assert.assertEquals(numberOfProps, group.size());
  }

  @Test
  public void testToPropGroup() {
    PropGroup propGroup =
        PropGroup.ofJson(
            "{"
                + "\"order\": 1,"
                + "\"props\": {"
                + "\"aa\": \"cc\", \"aa2\": \"cc2\""
                + "}"
                + "}");
    Assert.assertEquals(1, propGroup.order());
    Assert.assertEquals(2, propGroup.size());
    Assert.assertEquals("cc", propGroup.props().get("aa"));
    Assert.assertEquals("cc2", propGroup.props().get("aa2"));
  }

  @Test
  public void testToColumn() {
    Column column =
        Column.newBuilder()
            .order(10)
            .name(CommonUtils.randomString())
            .newName(CommonUtils.randomString())
            .dataType(DataType.BYTES)
            .build();
    Assert.assertEquals(column, PropGroup.of(column).toColumn());
  }
}
