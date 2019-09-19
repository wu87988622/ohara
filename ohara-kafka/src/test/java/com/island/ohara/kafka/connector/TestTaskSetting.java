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

import com.google.common.collect.ImmutableMap;
import com.island.ohara.common.rule.OharaTest;
import com.island.ohara.common.setting.PropGroups;
import com.island.ohara.common.setting.SettingDef;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.connector.json.StringList;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class TestTaskSetting extends OharaTest {

  @Test(expected = NullPointerException.class)
  public void nullInput() {
    TaskSetting.of(null);
  }

  @Test
  public void emptyInput() {
    TaskSetting.of(Collections.emptyMap());
  }

  @Test
  public void nullKey() {
    Map<String, String> map = Collections.singletonMap(null, CommonUtils.randomString());
    assertException(NullPointerException.class, () -> TaskSetting.of(map));
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyKey() {
    TaskSetting.of(Collections.singletonMap("", CommonUtils.randomString()));
  }

  @Test
  public void nullValue() {
    Map<String, String> map = Collections.singletonMap(CommonUtils.randomString(), null);
    assertException(NullPointerException.class, () -> TaskSetting.of(map));
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyValue() {
    TaskSetting.of(Collections.singletonMap(CommonUtils.randomString(), ""));
  }

  @Test
  public void testParseBoolean() {
    String key = CommonUtils.randomString();
    boolean value = true;
    TaskSetting config = TaskSetting.of(Collections.singletonMap(key, String.valueOf(value)));
    Assert.assertEquals(value, config.booleanValue(key));
    Assert.assertFalse(config.booleanOption(CommonUtils.randomString()).isPresent());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseNonBoolean() {
    String key = CommonUtils.randomString();
    TaskSetting config = TaskSetting.of(Collections.singletonMap(key, CommonUtils.randomString()));
    config.booleanValue(key);
  }

  @Test
  public void testParseShort() {
    String key = CommonUtils.randomString();
    short value = 123;
    TaskSetting config = TaskSetting.of(Collections.singletonMap(key, String.valueOf(value)));
    Assert.assertEquals(value, config.shortValue(key));
    Assert.assertTrue(config.shortOption(key).isPresent());
    Assert.assertFalse(config.shortOption(CommonUtils.randomString()).isPresent());
  }

  @Test
  public void testParseInt() {
    String key = CommonUtils.randomString();
    int value = 123;
    TaskSetting config = TaskSetting.of(Collections.singletonMap(key, String.valueOf(value)));
    Assert.assertEquals(value, config.intValue(key));
    Assert.assertTrue(config.intOption(key).isPresent());
    Assert.assertFalse(config.intOption(CommonUtils.randomString()).isPresent());
  }

  @Test
  public void testParseLong() {
    String key = CommonUtils.randomString();
    long value = 123;
    TaskSetting config = TaskSetting.of(Collections.singletonMap(key, String.valueOf(value)));
    Assert.assertEquals(value, config.longValue(key));
    Assert.assertTrue(config.longOption(key).isPresent());
    Assert.assertFalse(config.longOption(CommonUtils.randomString()).isPresent());
  }

  @Test
  public void testParseDouble() {
    String key = CommonUtils.randomString();
    double value = 123.333;
    TaskSetting config = TaskSetting.of(Collections.singletonMap(key, String.valueOf(value)));
    Assert.assertEquals(value, config.doubleValue(key), 0);
    Assert.assertTrue(config.doubleOption(key).isPresent());
    Assert.assertFalse(config.doubleOption(CommonUtils.randomString()).isPresent());
  }

  @Test
  public void testParseStrings() {
    String key = CommonUtils.randomString();
    List<String> ss = Arrays.asList(CommonUtils.randomString(), CommonUtils.randomString());
    TaskSetting config =
        TaskSetting.of(Collections.singletonMap(key, StringList.toKafkaString(ss)));
    List<String> ss2 = config.stringList(key);
    Assert.assertEquals(ss.size(), ss2.size());
    ss.forEach(s -> Assert.assertEquals(1, ss2.stream().filter(s::equals).count()));
    Assert.assertFalse(config.stringListOption(CommonUtils.randomString()).isPresent());
  }

  @Test
  public void testToPropGroups() {
    String key = CommonUtils.randomString();
    PropGroups propGroups =
        PropGroups.of(
            Arrays.asList(
                ImmutableMap.of("k0", "v0", "k1", "v1", "k2", "v2"),
                ImmutableMap.of("k0", "v0", "k1", "v1")));
    TaskSetting config = TaskSetting.of(ImmutableMap.of(key, propGroups.toJsonString()));
    PropGroups another = config.propGroups(key);
    Assert.assertEquals(propGroups, another);
    Assert.assertTrue(config.propGroupsOption(key).isPresent());
    Assert.assertFalse(config.propGroupsOption(CommonUtils.randomString()).isPresent());
  }

  @Test
  public void getEmptyColumn() {
    TaskSetting config = TaskSetting.of(ImmutableMap.of("pgs", "asdasd"));
    Assert.assertTrue(config.columns().isEmpty());
  }

  @Test
  public void testFillDefaultValue() {
    String key = CommonUtils.randomString();
    String defaultValue = CommonUtils.randomString();
    SettingDef settingDefinition = SettingDef.builder().key(key).optional(defaultValue).build();
    TaskSetting config =
        TaskSetting.of(Collections.emptyMap(), Collections.singletonList(settingDefinition));
    Assert.assertEquals(1, config.raw().size());
    Assert.assertEquals(defaultValue, config.stringValue(key));
  }

  @Test
  public void skipDefaultValueIfValueExists() {
    String key = CommonUtils.randomString();
    String value = CommonUtils.randomString();
    String defaultValue = CommonUtils.randomString();
    SettingDef settingDefinition = SettingDef.builder().key(key).optional(defaultValue).build();
    TaskSetting config =
        TaskSetting.of(
            Collections.singletonMap(key, value), Collections.singletonList(settingDefinition));
    Assert.assertEquals(1, config.raw().size());
    Assert.assertEquals(value, config.stringValue(key));
  }

  @Test
  public void testToDuration() {
    Duration duration = Duration.ofSeconds(10);
    Assert.assertEquals(duration, CommonUtils.toDuration(duration.toString()));
    Assert.assertEquals(duration, CommonUtils.toDuration("10 seconds"));
  }
}
