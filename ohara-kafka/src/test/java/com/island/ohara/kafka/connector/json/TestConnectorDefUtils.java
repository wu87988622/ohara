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
import com.island.ohara.common.rule.OharaTest;
import com.island.ohara.common.setting.PropGroup;
import com.island.ohara.common.setting.SettingDef;
import com.island.ohara.common.util.CommonUtils;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.runtime.rest.entities.ConfigKeyInfo;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TestConnectorDefUtils extends OharaTest {

  @Test
  public void testToConfigDefKey() {
    SettingDef settingDef =
        SettingDef.builder()
            .key(CommonUtils.randomString())
            .displayName(CommonUtils.randomString())
            .group(CommonUtils.randomString())
            .reference(SettingDef.Reference.WORKER_CLUSTER)
            .orderInGroup(111)
            .optional(CommonUtils.randomString())
            .documentation(CommonUtils.randomString())
            .build();
    Assert.assertEquals(settingDef.key(), ConnectorDefUtils.toConfigKey(settingDef).name);
    Assert.assertEquals(
        settingDef.valueType().name(), ConnectorDefUtils.toConfigKey(settingDef).type.name());
    Assert.assertEquals(settingDef.group(), ConnectorDefUtils.toConfigKey(settingDef).group);
    Assert.assertEquals(
        settingDef.orderInGroup(), ConnectorDefUtils.toConfigKey(settingDef).orderInGroup);
    Assert.assertEquals(
        settingDef.defaultString(), ConnectorDefUtils.toConfigKey(settingDef).defaultValue);
    Assert.assertEquals(
        settingDef.documentation(), ConnectorDefUtils.toConfigKey(settingDef).documentation);
  }

  @Test
  public void testToConfigDefKey2() {
    SettingDef settingDef =
        SettingDef.builder()
            .key(CommonUtils.randomString())
            .displayName(CommonUtils.randomString())
            .group(CommonUtils.randomString())
            .reference(SettingDef.Reference.WORKER_CLUSTER)
            .orderInGroup(111)
            .optional(CommonUtils.randomString())
            .documentation(CommonUtils.randomString())
            .build();

    SettingDef another =
        ConnectorDefUtils.of(convertConfigKey(ConnectorDefUtils.toConfigKey(settingDef)));
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
        ConnectorDefUtils.DEFAULT.stream()
            .filter(d -> d.key().equals(ConnectorDefUtils.KEY_CONVERTER_DEFINITION.key()))
            .findAny()
            .get()
            .defaultString());
  }

  @Test
  public void testDefaultValueConverter() {
    Assert.assertEquals(
        ConverterType.NONE.className(),
        ConnectorDefUtils.DEFAULT.stream()
            .filter(d -> d.key().equals(ConnectorDefUtils.VALUE_CONVERTER_DEFINITION.key()))
            .findAny()
            .get()
            .defaultString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseStaleConfigKeyInfo() {
    ConfigKeyInfo fake = Mockito.mock(ConfigKeyInfo.class);
    Mockito.when(fake.displayName()).thenReturn(CommonUtils.randomString());
    ConnectorDefUtils.of(fake);
  }

  @Test(expected = NullPointerException.class)
  public void parseStaleConfigKeyInfo2() {
    ConfigKeyInfo fake = Mockito.mock(ConfigKeyInfo.class);
    Mockito.when(fake.displayName()).thenReturn(null);
    ConnectorDefUtils.of(fake);
  }

  @Test
  public void testTableValidator() {
    SettingDef settingDef =
        SettingDef.builder()
            .key(CommonUtils.randomString())
            .optional(SettingDef.Type.TABLE)
            .build();
    ConfigDef.ConfigKey key = ConnectorDefUtils.toConfigKey(settingDef);
    Assert.assertNotNull(key.validator);
    key.validator.ensureValid(settingDef.key(), null);
    assertException(ConfigException.class, () -> key.validator.ensureValid(settingDef.key(), 123));
    assertException(
        ConfigException.class,
        () ->
            key.validator.ensureValid(
                settingDef.key(), Collections.singletonList(CommonUtils.randomString())));

    key.validator.ensureValid(
        settingDef.key(),
        PropGroup.ofColumns(
                Collections.singletonList(
                    Column.builder()
                        .name(CommonUtils.randomString())
                        .dataType(DataType.BOOLEAN)
                        .build()))
            .toJsonString());
  }

  @Test
  public void testTypeConversion() {
    Stream.of(SettingDef.Type.values()).forEach(ConnectorDefUtils::toType);
  }

  @Test
  public void checkReturnBySettingDefShouldBeSame() {
    ConnectorDefUtils.DEFAULT.forEach(setting -> Assert.assertNotNull(setting.checker()));
  }

  @Test
  public void testSerialization() {
    ConnectorDefUtils.DEFAULT.forEach(
        setting -> {
          SettingDef copy = (SettingDef) Serializer.OBJECT.from(Serializer.OBJECT.to(setting));
          Assert.assertEquals(setting, copy);
        });
  }

  @Test
  public void testConnectorClass() {
    Assert.assertEquals(
        1,
        ConnectorDefUtils.DEFAULT.stream()
            .filter(d -> d.equals(ConnectorDefUtils.CONNECTOR_CLASS_DEFINITION))
            .count());
  }

  @Test
  public void testTopics() {
    Assert.assertEquals(
        1,
        ConnectorDefUtils.DEFAULT.stream()
            .filter(d -> d.equals(ConnectorDefUtils.TOPIC_NAMES_DEFINITION))
            .count());
  }

  @Test
  public void testColumns() {
    Assert.assertEquals(
        1,
        ConnectorDefUtils.DEFAULT.stream()
            .filter(d -> d.equals(ConnectorDefUtils.COLUMNS_DEFINITION))
            .count());
  }

  @Test
  public void testKeyConverter() {
    Assert.assertEquals(
        1,
        ConnectorDefUtils.DEFAULT.stream()
            .filter(d -> d.equals(ConnectorDefUtils.KEY_CONVERTER_DEFINITION))
            .count());
  }

  @Test
  public void testValueConverter() {
    Assert.assertEquals(
        1,
        ConnectorDefUtils.DEFAULT.stream()
            .filter(d -> d.equals(ConnectorDefUtils.VALUE_CONVERTER_DEFINITION))
            .count());
  }

  @Test
  public void testWorkerClusterName() {
    Assert.assertEquals(
        1,
        ConnectorDefUtils.DEFAULT.stream()
            .filter(d -> d.equals(ConnectorDefUtils.WORKER_CLUSTER_KEY_DEFINITION))
            .count());
  }

  @Test
  public void testNumberOfTasks() {
    Assert.assertEquals(
        1,
        ConnectorDefUtils.DEFAULT.stream()
            .filter(d -> d.equals(ConnectorDefUtils.NUMBER_OF_TASKS_DEFINITION))
            .count());
  }

  @Test
  public void mustHaveTable() {
    Assert.assertEquals(
        1,
        ConnectorDefUtils.DEFAULT.stream()
            .filter(definition -> definition.valueType().equals(SettingDef.Type.TABLE))
            .count());
  }

  @Test
  public void testPropKeys() {
    ConnectorDefUtils.DEFAULT.forEach(
        definition -> {
          if (definition.valueType().equals(SettingDef.Type.TABLE)) {
            Assert.assertTrue(
                definition.tableKeys().stream()
                    .anyMatch(k -> k.name().equals(SettingDef.ORDER_KEY)));
            Assert.assertTrue(
                definition.tableKeys().stream()
                    .anyMatch(k -> k.name().equals(SettingDef.COLUMN_DATA_TYPE_KEY)));
            Assert.assertTrue(
                definition.tableKeys().stream()
                    .anyMatch(k -> k.name().equals(SettingDef.COLUMN_NAME_KEY)));
            Assert.assertTrue(
                definition.tableKeys().stream()
                    .anyMatch(k -> k.name().equals(SettingDef.COLUMN_NEW_NAME_KEY)));
          } else Assert.assertTrue(definition.tableKeys().isEmpty());
        });
  }

  @Test
  public void checkDuplicate() {
    Assert.assertEquals(
        ConnectorDefUtils.DEFAULT.size(),
        ConnectorDefUtils.DEFAULT.stream().map(SettingDef::key).collect(Collectors.toSet()).size());
  }

  @Test
  public void testConnectorNameSetting() {
    SettingDef setting =
        ConnectorDefUtils.DEFAULT.stream()
            .filter(s -> s.key().equals(ConnectorDefUtils.CONNECTOR_NAME_DEFINITION.key()))
            .findFirst()
            .get();
    Assert.assertEquals(setting.necessary(), SettingDef.Necessary.OPTIONAL_WITH_RANDOM_DEFAULT);
    Assert.assertFalse(setting.internal());
    Assert.assertFalse(setting.hasDefault());
    Assert.assertEquals(SettingDef.Reference.NONE, setting.reference());
    Assert.assertTrue(setting.tableKeys().isEmpty());
    Assert.assertEquals(ConnectorDefUtils.CORE_GROUP, setting.group());
  }

  @Test
  public void testConnectorKeySetting() {
    SettingDef setting =
        ConnectorDefUtils.DEFAULT.stream()
            .filter(s -> s.key().equals(ConnectorDefUtils.CONNECTOR_KEY_DEFINITION.key()))
            .findFirst()
            .get();
    Assert.assertEquals(setting.necessary(), SettingDef.Necessary.REQUIRED);
    Assert.assertTrue(setting.internal());
    Assert.assertFalse(setting.hasDefault());
    Assert.assertEquals(SettingDef.Reference.NONE, setting.reference());
    Assert.assertEquals(SettingDef.Type.OBJECT_KEY, setting.valueType());
    Assert.assertTrue(setting.tableKeys().isEmpty());
    Assert.assertEquals(ConnectorDefUtils.CORE_GROUP, setting.group());
  }

  @Test
  public void testTagsSetting() {
    SettingDef setting =
        ConnectorDefUtils.DEFAULT.stream()
            .filter(s -> s.key().equals(ConnectorDefUtils.TAGS_DEFINITION.key()))
            .findFirst()
            .get();
    Assert.assertEquals(setting.necessary(), SettingDef.Necessary.OPTIONAL);
    Assert.assertFalse(setting.internal());
    Assert.assertFalse(setting.hasDefault());
    Assert.assertEquals(SettingDef.Reference.NONE, setting.reference());
    Assert.assertTrue(setting.tableKeys().isEmpty());
    Assert.assertEquals(ConnectorDefUtils.CORE_GROUP, setting.group());
  }

  @Test
  public void testTopicKeysSetting() {
    SettingDef setting =
        ConnectorDefUtils.DEFAULT.stream()
            .filter(s -> s.key().equals(ConnectorDefUtils.TOPIC_KEYS_DEFINITION.key()))
            .findFirst()
            .get();
    Assert.assertEquals(setting.necessary(), SettingDef.Necessary.OPTIONAL);
    Assert.assertFalse(setting.internal());
    Assert.assertFalse(setting.hasDefault());
    Assert.assertEquals(SettingDef.Reference.TOPIC, setting.reference());
    Assert.assertTrue(setting.tableKeys().isEmpty());
    Assert.assertEquals(ConnectorDefUtils.CORE_GROUP, setting.group());
  }
}
