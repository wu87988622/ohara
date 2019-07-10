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

import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestSettingDef extends SmallTest {

  @Test(expected = NullPointerException.class)
  public void nullKey() {
    SettingDef.builder().key(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyKey() {
    SettingDef.builder().key("");
  }

  @Test(expected = NullPointerException.class)
  public void nullType() {
    SettingDef.builder().valueType(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullValueDefault() {
    SettingDef.builder().optional(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullDocumentation() {
    SettingDef.builder().documentation(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyDocumentation() {
    SettingDef.builder().documentation("");
  }

  @Test(expected = NullPointerException.class)
  public void nullReference() {
    SettingDef.builder().reference(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullGroup() {
    SettingDef.builder().group(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyGroup() {
    SettingDef.builder().group("");
  }

  @Test(expected = NullPointerException.class)
  public void nullDisplay() {
    SettingDef.builder().displayName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyDisplay() {
    SettingDef.builder().displayName("");
  }

  @Test
  public void testJson() {
    SettingDef def =
        SettingDef.builder()
            .key(CommonUtils.randomString())
            .displayName(CommonUtils.randomString())
            .group(CommonUtils.randomString())
            .documentation(CommonUtils.randomString())
            .build();

    Assert.assertEquals(def, SettingDef.ofJson(def.toString()));
  }

  @Test
  public void testOnlyKeyAndGroup() {
    String key = CommonUtils.randomString(5);
    String group = "default";
    SettingDef def = SettingDef.builder().key(key).group(group).build();
    Assert.assertEquals(key, def.key());
    Assert.assertEquals(group, def.group());
    Assert.assertNotNull(def.displayName());
    // default we use key as display name
    Assert.assertEquals(def.key(), def.displayName());
    Assert.assertNotNull(def.documentation());
    Assert.assertNotNull(def.valueType());
    Assert.assertNotNull(def.group());
    Assert.assertNotNull(def.reference());
    // yep. the default value should be null
    Assert.assertNull(def.defaultValue());
  }

  @Test
  public void testGetterWithEditableAndDefaultValue() {
    String key = CommonUtils.randomString(5);
    SettingDef.Type type = SettingDef.Type.TABLE;
    String displayName = CommonUtils.randomString(5);
    String group = CommonUtils.randomString(5);
    SettingDef.Reference reference = SettingDef.Reference.WORKER_CLUSTER;
    int orderInGroup = 100;
    String valueDefault = CommonUtils.randomString(5);
    String documentation = CommonUtils.randomString(5);
    SettingDef def =
        SettingDef.builder()
            .key(key)
            .valueType(type)
            .displayName(displayName)
            .group(group)
            .reference(reference)
            .orderInGroup(orderInGroup)
            .optional(valueDefault)
            .documentation(documentation)
            .build();

    Assert.assertEquals(key, def.key());
    Assert.assertEquals(type, def.valueType());
    Assert.assertEquals(displayName, def.displayName());
    Assert.assertEquals(group, def.group());
    Assert.assertEquals(reference, def.reference());
    Assert.assertEquals(orderInGroup, def.orderInGroup());
    Assert.assertEquals(valueDefault, def.defaultValue());
    Assert.assertEquals(documentation, def.documentation());
    Assert.assertFalse(def.required());
    Assert.assertTrue(def.editable());
    Assert.assertFalse(def.internal());
  }

  @Test
  public void testGetterWithoutEditableAndDefaultValue() {
    String key = CommonUtils.randomString(5);
    SettingDef.Type type = SettingDef.Type.TABLE;
    String displayName = CommonUtils.randomString(5);
    String group = CommonUtils.randomString(5);
    SettingDef.Reference reference = SettingDef.Reference.WORKER_CLUSTER;
    int orderInGroup = 100;
    String documentation = CommonUtils.randomString(5);
    SettingDef def =
        SettingDef.builder()
            .key(key)
            .valueType(type)
            .displayName(displayName)
            .group(group)
            .reference(reference)
            .orderInGroup(orderInGroup)
            .optional()
            .documentation(documentation)
            .readonly()
            .internal()
            .build();

    Assert.assertEquals(key, def.key());
    Assert.assertEquals(type, def.valueType());
    Assert.assertEquals(displayName, def.displayName());
    Assert.assertEquals(group, def.group());
    Assert.assertEquals(reference, def.reference());
    Assert.assertEquals(orderInGroup, def.orderInGroup());
    Assert.assertNull(def.defaultValue());
    Assert.assertEquals(documentation, def.documentation());
    Assert.assertFalse(def.required());
    Assert.assertFalse(def.editable());
    Assert.assertTrue(def.internal());
  }

  @Test
  public void testSetDisplayName() {
    String displayName = CommonUtils.randomString();
    SettingDef def =
        SettingDef.builder()
            .displayName(displayName)
            .key(CommonUtils.randomString())
            .group(CommonUtils.randomString())
            .valueType(SettingDef.Type.STRING)
            .build();
    Assert.assertEquals(displayName, def.displayName());
  }
}
