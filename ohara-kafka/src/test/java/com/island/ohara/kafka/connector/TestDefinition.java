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

package com.island.ohara.kafka.connector;

import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtil;
import org.junit.Assert;
import org.junit.Test;

public class TestDefinition extends SmallTest {

  @Test(expected = NullPointerException.class)
  public void nullName() {
    Definition.newBuilder().name(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyName() {
    Definition.newBuilder().name("");
  }

  @Test(expected = NullPointerException.class)
  public void nullType() {
    Definition.newBuilder().valueType(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullValueDefault() {
    Definition.newBuilder().optional(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullDocumentation() {
    Definition.newBuilder().documentation(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyDocumentation() {
    Definition.newBuilder().documentation("");
  }

  @Test
  public void testEqualWithValueDefault() {
    String name = CommonUtil.randomString(5);
    String documentation = CommonUtil.randomString(5);
    Definition def =
        Definition.newBuilder()
            .name(name)
            .valueType(Definition.Type.STRING)
            .documentation(documentation)
            .build();

    Assert.assertEquals(name, def.name());
    Assert.assertEquals(Definition.Type.STRING, def.valueType());
    Assert.assertEquals(documentation, def.documentation());
    Assert.assertFalse(def.valueDefault().isPresent());
  }

  @Test
  public void testEqualWithoutValueDefault() {
    String name = CommonUtil.randomString(5);
    String documentation = CommonUtil.randomString(5);
    String valueDefault = CommonUtil.randomString(5);
    Definition def =
        Definition.newBuilder()
            .name(name)
            .valueType(Definition.Type.STRING)
            .optional(valueDefault)
            .documentation(documentation)
            .build();

    Assert.assertEquals(name, def.name());
    Assert.assertEquals(Definition.Type.STRING, def.valueType());
    Assert.assertTrue(def.valueDefault().isPresent());
    Assert.assertEquals(valueDefault, def.valueDefault().get());
    Assert.assertEquals(documentation, def.documentation());
  }
}
