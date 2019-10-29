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

package com.island.ohara.streams.config;

import com.island.ohara.common.rule.OharaTest;
import com.island.ohara.common.setting.SettingDef;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.streams.StreamTestUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.junit.Test;

public class TestStreamDefUtils extends OharaTest {

  @Test
  public void testConfigJson() {
    StreamDefinitions defaultConfig = StreamDefinitions.create();
    StreamDefinitions config = StreamDefUtils.ofJson(StreamDefUtils.toJson(defaultConfig));

    Assert.assertEquals(StreamDefUtils.toJson(defaultConfig), StreamDefUtils.toJson(config));
    Assert.assertEquals(
        "default config size not equal",
        config.getSettingDefList().size(),
        defaultConfig.getSettingDefList().size());

    StreamDefinitions another =
        StreamDefinitions.with(
            SettingDef.builder()
                .key(CommonUtils.randomString())
                .group(CommonUtils.randomString())
                .build());
    Assert.assertEquals(
        StreamDefUtils.toJson(another),
        StreamDefUtils.toJson(StreamDefUtils.ofJson(StreamDefUtils.toJson(another))));
  }

  @Test
  public void testAddConfig() {
    String key = CommonUtils.randomString();
    String group = "default";

    StreamDefinitions newConfigs =
        StreamDefinitions.with(SettingDef.builder().key(key).group(group).build());

    Assert.assertEquals(
        newConfigs.getSettingDefList().size(),
        StreamDefinitions.create().getSettingDefList().size() + 1);
    Assert.assertTrue(newConfigs.keys().contains(key));

    List<SettingDef> list = new ArrayList<>();
    IntStream.rangeClosed(1, 10)
        .boxed()
        .map(String::valueOf)
        .forEach(i -> list.add(SettingDef.builder().key(String.valueOf(i)).group(group).build()));
    StreamDefinitions newConfigList = StreamDefinitions.withAll(list);
    Assert.assertEquals(
        newConfigList.getSettingDefList().size(),
        StreamDefinitions.create().getSettingDefList().size() + 10);
    Assert.assertTrue(
        newConfigList
            .keys()
            .containsAll(
                IntStream.rangeClosed(1, 10)
                    .boxed()
                    .map(String::valueOf)
                    .collect(Collectors.toList())));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDuplicateKey() {
    StreamDefinitions.with(
        SettingDef.builder().key(StreamDefUtils.NAME_DEFINITION.key()).group("c").build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDuplicatePartialKey() {
    List<SettingDef> list =
        IntStream.rangeClosed(1, 10)
            .boxed()
            .map(String::valueOf)
            .map(
                i ->
                    SettingDef.builder()
                        .key(String.valueOf(StreamDefUtils.BROKER_DEFINITION.key()))
                        .group(i)
                        .build())
            .collect(Collectors.toList());

    StreamDefinitions.withAll(list);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testForbiddenModifyDefs() {
    // we don't allow change the internal definitions, use with() or withAll() instead
    StreamDefinitions.create()
        .getSettingDefList()
        .add(SettingDef.builder().key(CommonUtils.randomString()).build());
  }

  @Test
  public void testJarDefinition() {
    Assert.assertEquals(StreamDefUtils.JAR_KEY_DEFINITION.valueType(), SettingDef.Type.OBJECT_KEY);
    Assert.assertEquals(StreamDefUtils.JAR_KEY_DEFINITION.reference(), SettingDef.Reference.FILE);
  }

  @Test
  public void testGetStringFromEnv() {
    SettingDef settingDef = SettingDef.builder().key("aaa").build();
    StreamDefinitions configs = StreamDefinitions.with(settingDef);
    StreamTestUtils.setOharaEnv(Collections.singletonMap("aaa", "bbb"));

    // should be defined in env
    Assert.assertTrue(
        "key: aaa not found in env!",
        configs.string("aaa").isPresent() && configs.string("aaa").get().equals("bbb"));

    // should not have such key in env
    Assert.assertFalse(
        "absent key should not have value", configs.string(CommonUtils.randomString()).isPresent());
  }
}
