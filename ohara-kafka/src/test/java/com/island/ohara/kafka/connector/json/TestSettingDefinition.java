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
import com.island.ohara.common.util.CommonUtils;
import java.util.List;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.runtime.rest.entities.ConfigKeyInfo;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TestSettingDefinition extends SmallTest {

  @Test(expected = NullPointerException.class)
  public void nullKey() {
    SettingDefinition.newBuilder().key(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyKey() {
    SettingDefinition.newBuilder().key("");
  }

  @Test(expected = NullPointerException.class)
  public void nullType() {
    SettingDefinition.newBuilder().valueType(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullValueDefault() {
    SettingDefinition.newBuilder().optional(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullDocumentation() {
    SettingDefinition.newBuilder().documentation(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyDocumentation() {
    SettingDefinition.newBuilder().documentation("");
  }

  @Test(expected = NullPointerException.class)
  public void nullReference() {
    SettingDefinition.newBuilder().reference(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullGroup() {
    SettingDefinition.newBuilder().group(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyGroup() {
    SettingDefinition.newBuilder().group("");
  }

  @Test(expected = NullPointerException.class)
  public void nullDisplay() {
    SettingDefinition.newBuilder().displayName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyDisplay() {
    SettingDefinition.newBuilder().displayName("");
  }

  @Test
  public void testOnlyKey() {
    String key = CommonUtils.randomString(5);
    SettingDefinition def = SettingDefinition.newBuilder().key(key).build();
    Assert.assertEquals(key, def.key());
    Assert.assertNotNull(def.displayName());
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
    SettingDefinition.Type type = SettingDefinition.Type.TABLE;
    String displayName = CommonUtils.randomString(5);
    String group = CommonUtils.randomString(5);
    SettingDefinition.Reference reference = SettingDefinition.Reference.WORKER_CLUSTER;
    int orderInGroup = 100;
    String valueDefault = CommonUtils.randomString(5);
    String documentation = CommonUtils.randomString(5);
    SettingDefinition def =
        SettingDefinition.newBuilder()
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
    Assert.assertEquals(type.name(), def.valueType());
    Assert.assertEquals(displayName, def.displayName());
    Assert.assertEquals(group, def.group());
    Assert.assertEquals(reference.name(), def.reference());
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
    SettingDefinition.Type type = SettingDefinition.Type.TABLE;
    String displayName = CommonUtils.randomString(5);
    String group = CommonUtils.randomString(5);
    SettingDefinition.Reference reference = SettingDefinition.Reference.WORKER_CLUSTER;
    int orderInGroup = 100;
    String documentation = CommonUtils.randomString(5);
    SettingDefinition def =
        SettingDefinition.newBuilder()
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
    Assert.assertEquals(type.name(), def.valueType());
    Assert.assertEquals(displayName, def.displayName());
    Assert.assertEquals(group, def.group());
    Assert.assertEquals(reference.name(), def.reference());
    Assert.assertEquals(orderInGroup, def.orderInGroup());
    Assert.assertNull(def.defaultValue());
    Assert.assertEquals(documentation, def.documentation());
    Assert.assertFalse(def.required());
    Assert.assertFalse(def.editable());
    Assert.assertTrue(def.internal());
  }

  @Test
  public void testToConfigDefKey() {
    SettingDefinition settingDefinition =
        SettingDefinition.newBuilder()
            .key(CommonUtils.randomString())
            .valueType(SettingDefinition.Type.STRING)
            .displayName(CommonUtils.randomString())
            .group(CommonUtils.randomString())
            .reference(SettingDefinition.Reference.WORKER_CLUSTER)
            .orderInGroup(111)
            .optional(CommonUtils.randomString())
            .documentation(CommonUtils.randomString())
            .build();
    Assert.assertEquals(settingDefinition.key(), settingDefinition.toConfigKey().name);
    Assert.assertEquals(settingDefinition.valueType(), settingDefinition.toConfigKey().type.name());
    Assert.assertEquals(settingDefinition.group(), settingDefinition.toConfigKey().group);
    Assert.assertEquals(
        settingDefinition.orderInGroup(), settingDefinition.toConfigKey().orderInGroup);
    Assert.assertEquals(
        settingDefinition.defaultValue(), settingDefinition.toConfigKey().defaultValue);
    Assert.assertEquals(
        settingDefinition.documentation(), settingDefinition.toConfigKey().documentation);
  }

  @Test
  public void testToConfigDefKey2() {
    SettingDefinition settingDefinition =
        SettingDefinition.newBuilder()
            .key(CommonUtils.randomString())
            .valueType(SettingDefinition.Type.STRING)
            .displayName(CommonUtils.randomString())
            .group(CommonUtils.randomString())
            .reference(SettingDefinition.Reference.WORKER_CLUSTER)
            .orderInGroup(111)
            .optional(CommonUtils.randomString())
            .documentation(CommonUtils.randomString())
            .build();

    SettingDefinition another =
        SettingDefinition.of(convertConfigKey(settingDefinition.toConfigKey()));
    Assert.assertEquals(settingDefinition, another);
  }

  /**
   * This method is clone from kafka 1.0.2 source code. We need this method to test our conversion.
   */
  private static ConfigKeyInfo convertConfigKey(ConfigDef.ConfigKey configKey) {
    String name = configKey.name;
    ConfigDef.Type type = configKey.type;
    String typeName = configKey.type.name();

    boolean required = false;
    String defaultValue;
    if (ConfigDef.NO_DEFAULT_VALUE.equals(configKey.defaultValue)) {
      defaultValue = null;
      required = true;
    } else {
      defaultValue = ConfigDef.convertToString(configKey.defaultValue, type);
    }
    String importance = configKey.importance.name();
    String documentation = configKey.documentation;
    String group = configKey.group;
    int orderInGroup = configKey.orderInGroup;
    String width = configKey.width.name();
    String displayName = configKey.displayName;
    List<String> dependents = configKey.dependents;
    return new ConfigKeyInfo(
        name,
        typeName,
        required,
        defaultValue,
        importance,
        documentation,
        group,
        orderInGroup,
        width,
        displayName,
        dependents);
  }

  @Test
  public void testDefaultKeyConverter() {
    Assert.assertEquals(
        ConverterType.NONE.className(),
        SettingDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.key().equals(SettingDefinition.KEY_CONVERTER_DEFINITION.key()))
            .findAny()
            .get()
            .defaultValue());
  }

  @Test
  public void testDefaultValueConverter() {
    Assert.assertEquals(
        ConverterType.NONE.className(),
        SettingDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.key().equals(SettingDefinition.VALUE_CONVERTER_DEFINITION.key()))
            .findAny()
            .get()
            .defaultValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseStaleConfigKeyInfo() {
    ConfigKeyInfo fake = Mockito.mock(ConfigKeyInfo.class);
    Mockito.when(fake.displayName()).thenReturn(CommonUtils.randomString());
    SettingDefinition.of(fake);
  }

  @Test(expected = NullPointerException.class)
  public void parseStaleConfigKeyInfo2() {
    ConfigKeyInfo fake = Mockito.mock(ConfigKeyInfo.class);
    Mockito.when(fake.displayName()).thenReturn(null);
    SettingDefinition.of(fake);
  }

  @Test
  public void testCopy() {
    SettingDefinitions.DEFINITIONS_DEFAULT.forEach(
        d ->
            Assert.assertEquals(
                d.toJsonString(), SettingDefinition.newBuilder(d).build().toJsonString()));
  }
}
