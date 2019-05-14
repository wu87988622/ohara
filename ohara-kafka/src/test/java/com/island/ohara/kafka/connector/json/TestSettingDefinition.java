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
import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtils;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.runtime.rest.entities.ConfigKeyInfo;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TestSettingDefinition extends SmallTest {

  @Test(expected = NullPointerException.class)
  public void nullKey() {
    SettingDefinition.builder().key(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyKey() {
    SettingDefinition.builder().key("");
  }

  @Test(expected = NullPointerException.class)
  public void nullType() {
    SettingDefinition.builder().valueType(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullValueDefault() {
    SettingDefinition.builder().optional(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullDocumentation() {
    SettingDefinition.builder().documentation(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyDocumentation() {
    SettingDefinition.builder().documentation("");
  }

  @Test(expected = NullPointerException.class)
  public void nullReference() {
    SettingDefinition.builder().reference(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullGroup() {
    SettingDefinition.builder().group(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyGroup() {
    SettingDefinition.builder().group("");
  }

  @Test(expected = NullPointerException.class)
  public void nullDisplay() {
    SettingDefinition.builder().displayName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyDisplay() {
    SettingDefinition.builder().displayName("");
  }

  @Test
  public void testOnlyKey() {
    String key = CommonUtils.randomString(5);
    SettingDefinition def = SettingDefinition.builder().key(key).build();
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
        SettingDefinition.builder()
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
        SettingDefinition.builder()
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
        SettingDefinition.builder()
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
        SettingDefinition.builder()
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
                d.toJsonString(), SettingDefinition.builder(d).build().toJsonString()));
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
    SettingDefinition.Builder builder =
        SettingDefinition.builder()
            .key(CommonUtils.randomString())
            .valueType(SettingDefinition.Type.TABLE);
    SettingDefinition settingDefinition = optional ? builder.optional().build() : builder.build();
    ConfigDef.ConfigKey key = settingDefinition.toConfigKey();
    Assert.assertNotNull(key.validator);
    if (optional) key.validator.ensureValid(settingDefinition.key(), null);
    else
      assertException(
          ConfigException.class, () -> key.validator.ensureValid(settingDefinition.key(), null));
    assertException(
        ConfigException.class, () -> key.validator.ensureValid(settingDefinition.key(), 123));
    assertException(
        ConfigException.class,
        () ->
            key.validator.ensureValid(
                settingDefinition.key(), Collections.singletonList(CommonUtils.randomString())));

    key.validator.ensureValid(
        settingDefinition.key(),
        PropGroups.ofColumns(
                Collections.singletonList(
                    Column.builder()
                        .name(CommonUtils.randomString())
                        .dataType(DataType.BOOLEAN)
                        .build()))
            .toJsonString());
  }

  @Test
  public void testTableChecker() {
    SettingDefinition settingDefinition =
        SettingDefinition.builder()
            .key(CommonUtils.randomString())
            .valueType(SettingDefinition.Type.TABLE)
            .tableKeys(Arrays.asList("a", "b"))
            .build();
    assertException(Exception.class, () -> settingDefinition.checker().check(null));
    assertException(Exception.class, () -> settingDefinition.checker().check(123));
    assertException(
        Exception.class, () -> settingDefinition.checker().check(Collections.emptyList()));
    assertException(
        Exception.class,
        () ->
            settingDefinition
                .checker()
                .check(Collections.singletonList(Collections.singletonMap("a", "c"))));
    settingDefinition
        .checker()
        .check(
            PropGroups.of(
                    Collections.singletonList(
                        settingDefinition.tableKeys().stream()
                            .collect(Collectors.toMap(Function.identity(), Function.identity()))))
                .toJsonString());
  }

  @Test
  public void testDurationChecker() {
    SettingDefinition settingDefinition =
        SettingDefinition.builder()
            .key(CommonUtils.randomString())
            .valueType(SettingDefinition.Type.DURATION)
            .build();
    assertException(Exception.class, () -> settingDefinition.checker().check(null));
    assertException(Exception.class, () -> settingDefinition.checker().check(123));
    assertException(
        Exception.class, () -> settingDefinition.checker().check(Collections.emptyList()));
    settingDefinition.checker().check(Duration.ofHours(3).toString());
    settingDefinition.checker().check("10 MILLISECONDS");
    settingDefinition.checker().check("10 SECONDS");
  }

  @Test
  public void testCustomChecker() {
    SettingDefinition settingDefinition =
        SettingDefinition.builder()
            .key(CommonUtils.randomString())
            .valueType(SettingDefinition.Type.STRING)
            .checker(
                v -> {
                  throw new RuntimeException();
                })
            .build();
    assertException(RuntimeException.class, () -> settingDefinition.checker().check("asdasd"));
  }

  @Test
  public void legalNullInValidator() {
    SettingDefinition settingDefinition =
        SettingDefinition.builder()
            .key(CommonUtils.randomString())
            .valueType(SettingDefinition.Type.STRING)
            .optional()
            .checker(
                v -> {
                  throw new RuntimeException();
                })
            .build();
    // this setting has optional value so it accept null value
    settingDefinition.toConfigKey().validator.ensureValid("aasd", null);
  }

  @Test
  public void illegalNullInValidator() {
    SettingDefinition settingDefinition =
        SettingDefinition.builder()
            .key(CommonUtils.randomString())
            .valueType(SettingDefinition.Type.STRING)
            .checker(
                v -> {
                  throw new RuntimeException();
                })
            .build();
    // this setting requires input

    assertException(
        RuntimeException.class,
        () -> settingDefinition.toConfigKey().validator.ensureValid("aasd", null));
  }

  @Test
  public void testTypeConversion() {
    Stream.of(SettingDefinition.Type.values()).forEach(SettingDefinition::toType);
  }
}
