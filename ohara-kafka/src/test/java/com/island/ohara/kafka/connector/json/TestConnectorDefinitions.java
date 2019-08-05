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

import com.island.ohara.common.data.Column;
import com.island.ohara.common.data.DataType;
import com.island.ohara.common.data.Serializer;
import com.island.ohara.common.exception.OharaConfigException;
import com.island.ohara.common.json.JsonUtils;
import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.setting.PropGroups;
import com.island.ohara.common.setting.SettingDef;
import com.island.ohara.common.util.CommonUtils;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.runtime.rest.entities.ConfigKeyInfo;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TestConnectorDefinitions extends SmallTest {

  @Test(expected = NoSuchElementException.class)
  public void noVersion() {
    ConnectorDefinitions.version(ConnectorDefinitions.DEFINITIONS_DEFAULT);
  }

  @Test(expected = NoSuchElementException.class)
  public void noRevision() {
    ConnectorDefinitions.revision(ConnectorDefinitions.DEFINITIONS_DEFAULT);
  }

  @Test(expected = NoSuchElementException.class)
  public void noAuthor() {
    ConnectorDefinitions.author(ConnectorDefinitions.DEFINITIONS_DEFAULT);
  }

  @Test
  public void testToConfigDefKey() {
    SettingDef settingDef =
        SettingDef.builder()
            .key(CommonUtils.randomString())
            .valueType(SettingDef.Type.STRING)
            .displayName(CommonUtils.randomString())
            .group(CommonUtils.randomString())
            .reference(SettingDef.Reference.WORKER_CLUSTER)
            .orderInGroup(111)
            .optional(CommonUtils.randomString())
            .documentation(CommonUtils.randomString())
            .build();
    Assert.assertEquals(settingDef.key(), ConnectorDefinitions.toConfigKey(settingDef).name);
    Assert.assertEquals(
        settingDef.valueType().name(), ConnectorDefinitions.toConfigKey(settingDef).type.name());
    Assert.assertEquals(settingDef.group(), ConnectorDefinitions.toConfigKey(settingDef).group);
    Assert.assertEquals(
        settingDef.orderInGroup(), ConnectorDefinitions.toConfigKey(settingDef).orderInGroup);
    Assert.assertEquals(
        settingDef.defaultValue(), ConnectorDefinitions.toConfigKey(settingDef).defaultValue);
    Assert.assertEquals(
        settingDef.documentation(), ConnectorDefinitions.toConfigKey(settingDef).documentation);
  }

  @Test
  public void testToConfigDefKey2() {
    SettingDef settingDef =
        SettingDef.builder()
            .key(CommonUtils.randomString())
            .valueType(SettingDef.Type.STRING)
            .displayName(CommonUtils.randomString())
            .group(CommonUtils.randomString())
            .reference(SettingDef.Reference.WORKER_CLUSTER)
            .orderInGroup(111)
            .optional(CommonUtils.randomString())
            .documentation(CommonUtils.randomString())
            .build();

    SettingDef another =
        ConnectorDefinitions.of(convertConfigKey(ConnectorDefinitions.toConfigKey(settingDef)));
    Assert.assertEquals(settingDef, another);
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
        ConnectorDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.key().equals(ConnectorDefinitions.KEY_CONVERTER_DEFINITION.key()))
            .findAny()
            .get()
            .defaultValue());
  }

  @Test
  public void testDefaultValueConverter() {
    Assert.assertEquals(
        ConverterType.NONE.className(),
        ConnectorDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.key().equals(ConnectorDefinitions.VALUE_CONVERTER_DEFINITION.key()))
            .findAny()
            .get()
            .defaultValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseStaleConfigKeyInfo() {
    ConfigKeyInfo fake = Mockito.mock(ConfigKeyInfo.class);
    Mockito.when(fake.displayName()).thenReturn(CommonUtils.randomString());
    ConnectorDefinitions.of(fake);
  }

  @Test(expected = NullPointerException.class)
  public void parseStaleConfigKeyInfo2() {
    ConfigKeyInfo fake = Mockito.mock(ConfigKeyInfo.class);
    Mockito.when(fake.displayName()).thenReturn(null);
    ConnectorDefinitions.of(fake);
  }

  @Test
  public void testCopy() {
    ConnectorDefinitions.DEFINITIONS_DEFAULT.forEach(
        d -> Assert.assertEquals(d.toJsonString(), SettingDef.builder(d).build().toJsonString()));
  }

  @Test
  public void testTableValidatorWithRequired() {
    testTableValidator(false);
  }

  @Test
  public void testTableValidatorWithOptional() {
    testTableValidator(true);
  }

  private void testTableValidator(boolean optional) {
    SettingDef.Builder builder =
        SettingDef.builder().key(CommonUtils.randomString()).valueType(SettingDef.Type.TABLE);
    SettingDef settingDef = optional ? builder.optional().build() : builder.build();
    ConfigDef.ConfigKey key = ConnectorDefinitions.toConfigKey(settingDef);
    Assert.assertNotNull(key.validator);
    if (optional) key.validator.ensureValid(settingDef.key(), null);
    else
      assertException(
          ConfigException.class, () -> key.validator.ensureValid(settingDef.key(), null));
    assertException(ConfigException.class, () -> key.validator.ensureValid(settingDef.key(), 123));
    assertException(
        ConfigException.class,
        () ->
            key.validator.ensureValid(
                settingDef.key(), Collections.singletonList(CommonUtils.randomString())));

    key.validator.ensureValid(
        settingDef.key(),
        PropGroups.ofColumns(
                Collections.singletonList(
                    Column.builder()
                        .name(CommonUtils.randomString())
                        .dataType(DataType.BOOLEAN)
                        .build()))
            .toJsonString());
  }

  @Test
  public void testTypeConversion() {
    Stream.of(SettingDef.Type.values()).forEach(ConnectorDefinitions::toType);
  }

  @Test
  public void checkReturnBySettingDefShouldBeSame() {
    ConnectorDefinitions.DEFINITIONS_DEFAULT.forEach(
        setting -> Assert.assertNotNull(setting.checker()));
  }

  @Test
  public void testTopicKeysType() {
    // pass
    ConnectorDefinitions.check(
        ConnectorDefinitions.chekerOfTopicKeys,
        JsonUtils.toString(
            Collections.singleton(
                TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString()))));
    // empty array is illegal
    assertException(
        OharaConfigException.class,
        () -> ConnectorDefinitions.check(ConnectorDefinitions.chekerOfTopicKeys, "[]"));
    assertException(
        OharaConfigException.class,
        () -> ConnectorDefinitions.check(ConnectorDefinitions.chekerOfTopicKeys, "{}"));
    assertException(
        OharaConfigException.class,
        () ->
            ConnectorDefinitions.check(
                ConnectorDefinitions.chekerOfTopicKeys, CommonUtils.randomString()));
    assertException(
        OharaConfigException.class,
        () -> ConnectorDefinitions.check(ConnectorDefinitions.chekerOfTopicKeys, 100000000));
  }

  @Test
  public void testConnectorKeyType() {
    // pass
    ConnectorDefinitions.check(
        ConnectorDefinitions.chekerOfConnectorKey,
        JsonUtils.toString(
            ConnectorKey.of(CommonUtils.randomString(), CommonUtils.randomString())));
    // empty array is illegal
    assertException(
        OharaConfigException.class,
        () -> ConnectorDefinitions.check(ConnectorDefinitions.chekerOfConnectorKey, "{}"));
    assertException(
        OharaConfigException.class,
        () ->
            ConnectorDefinitions.check(
                ConnectorDefinitions.chekerOfConnectorKey, CommonUtils.randomString()));
    assertException(
        OharaConfigException.class,
        () -> ConnectorDefinitions.check(ConnectorDefinitions.chekerOfConnectorKey, 100000000));
  }

  @Test
  public void testSerialization() {
    ConnectorDefinitions.DEFINITIONS_DEFAULT.forEach(
        setting -> {
          SettingDef copy = (SettingDef) Serializer.OBJECT.from(Serializer.OBJECT.to(setting));
          Assert.assertEquals(setting, copy);
        });
  }

  @Test
  public void testConnectorClass() {
    Assert.assertEquals(
        1,
        ConnectorDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.equals(ConnectorDefinitions.CONNECTOR_CLASS_DEFINITION))
            .count());
  }

  @Test
  public void testTopics() {
    Assert.assertEquals(
        1,
        ConnectorDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.equals(ConnectorDefinitions.TOPIC_NAMES_DEFINITION))
            .count());
  }

  @Test
  public void testColumns() {
    Assert.assertEquals(
        1,
        ConnectorDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.equals(ConnectorDefinitions.COLUMNS_DEFINITION))
            .count());
  }

  @Test
  public void testKeyConverter() {
    Assert.assertEquals(
        1,
        ConnectorDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.equals(ConnectorDefinitions.KEY_CONVERTER_DEFINITION))
            .count());
  }

  @Test
  public void testValueConverter() {
    Assert.assertEquals(
        1,
        ConnectorDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.equals(ConnectorDefinitions.VALUE_CONVERTER_DEFINITION))
            .count());
  }

  @Test
  public void testWorkerClusterName() {
    Assert.assertEquals(
        1,
        ConnectorDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.equals(ConnectorDefinitions.WORKER_CLUSTER_NAME_DEFINITION))
            .count());
  }

  @Test
  public void testNumberOfTasks() {
    Assert.assertEquals(
        1,
        ConnectorDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(d -> d.equals(ConnectorDefinitions.NUMBER_OF_TASKS_DEFINITION))
            .count());
  }

  @Test
  public void mustHaveTable() {
    Assert.assertEquals(
        1,
        ConnectorDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(definition -> definition.valueType().equals(SettingDef.Type.TABLE))
            .count());
  }

  @Test
  public void testPropKeys() {
    ConnectorDefinitions.DEFINITIONS_DEFAULT.forEach(
        definition -> {
          if (definition.valueType().equals(SettingDef.Type.TABLE)) {
            Assert.assertTrue(definition.tableKeys().contains(SettingDef.ORDER_KEY));
            Assert.assertTrue(definition.tableKeys().contains(SettingDef.COLUMN_DATA_TYPE_KEY));
            Assert.assertTrue(definition.tableKeys().contains(SettingDef.COLUMN_NAME_KEY));
            Assert.assertTrue(definition.tableKeys().contains(SettingDef.COLUMN_NEW_NAME_KEY));
          } else Assert.assertTrue(definition.tableKeys().isEmpty());
        });
  }

  @Test
  public void checkDuplicate() {
    Assert.assertEquals(
        ConnectorDefinitions.DEFINITIONS_DEFAULT.size(),
        ConnectorDefinitions.DEFINITIONS_DEFAULT.stream()
            .map(SettingDef::key)
            .collect(Collectors.toSet())
            .size());
  }

  @Test
  public void testConnectorNameSetting() {
    SettingDef setting =
        ConnectorDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(s -> s.key().equals(ConnectorDefinitions.CONNECTOR_NAME_DEFINITION.key()))
            .findFirst()
            .get();
    Assert.assertTrue(setting.required());
    Assert.assertTrue(setting.internal());
    Assert.assertNull(setting.defaultValue());
    Assert.assertEquals(SettingDef.Reference.NONE, setting.reference());
    Assert.assertTrue(setting.tableKeys().isEmpty());
    Assert.assertEquals(ConnectorDefinitions.CORE_GROUP, setting.group());
  }

  @Test
  public void testConnectorKeySetting() {
    SettingDef setting =
        ConnectorDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(s -> s.key().equals(ConnectorDefinitions.CONNECTOR_KEY_DEFINITION.key()))
            .findFirst()
            .get();
    Assert.assertTrue(setting.required());
    Assert.assertFalse(setting.internal());
    Assert.assertNull(setting.defaultValue());
    Assert.assertEquals(SettingDef.Reference.NONE, setting.reference());
    Assert.assertEquals(SettingDef.Type.CONNECTOR_KEY, setting.valueType());
    Assert.assertTrue(setting.tableKeys().isEmpty());
    Assert.assertEquals(ConnectorDefinitions.CORE_GROUP, setting.group());
  }

  @Test
  public void testTagsSetting() {
    SettingDef setting =
        ConnectorDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(s -> s.key().equals(ConnectorDefinitions.TAGS_DEFINITION.key()))
            .findFirst()
            .get();
    Assert.assertFalse(setting.required());
    Assert.assertTrue(setting.internal());
    Assert.assertNull(setting.defaultValue());
    Assert.assertEquals(SettingDef.Reference.NONE, setting.reference());
    Assert.assertTrue(setting.tableKeys().isEmpty());
    Assert.assertEquals(ConnectorDefinitions.CORE_GROUP, setting.group());
  }

  @Test
  public void testTopicKeysSetting() {
    SettingDef setting =
        ConnectorDefinitions.DEFINITIONS_DEFAULT.stream()
            .filter(s -> s.key().equals(ConnectorDefinitions.TOPIC_KEYS_DEFINITION.key()))
            .findFirst()
            .get();
    Assert.assertTrue(setting.required());
    Assert.assertFalse(setting.internal());
    Assert.assertNull(setting.defaultValue());
    Assert.assertEquals(SettingDef.Reference.TOPIC, setting.reference());
    Assert.assertTrue(setting.tableKeys().isEmpty());
    Assert.assertEquals(ConnectorDefinitions.CORE_GROUP, setting.group());
  }
}
