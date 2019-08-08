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

import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.setting.SettingDef;
import com.island.ohara.common.util.CommonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.junit.Test;

public class TestStreamDefinitions extends SmallTest {

  @Test
  public void testConfigJson() {
    StreamDefinitions defaultConfig = StreamDefinitions.create();
    StreamDefinitions config = StreamDefinitions.ofJson(defaultConfig.toString());

    Assert.assertEquals(defaultConfig, config);
    Assert.assertEquals(defaultConfig, config);
    Assert.assertEquals(
        "default config size not equal", config.values().size(), defaultConfig.values().size());

    StreamDefinitions another =
        StreamDefinitions.create()
            .add(
                SettingDef.builder()
                    .key(CommonUtils.randomString())
                    .group(CommonUtils.randomString())
                    .build());
    Assert.assertEquals(
        another.toString(), StreamDefinitions.ofJson(another.toString()).toString());
  }

  @Test
  public void testAddConfig() {
    String key = CommonUtils.randomString();
    String group = "default";

    StreamDefinitions newConfigs =
        StreamDefinitions.create().add(SettingDef.builder().key(key).group(group).build());
    Assert.assertEquals(newConfigs.values().size(), StreamDefinitions.create().values().size() + 1);
    Assert.assertTrue(newConfigs.keys().contains(key));

    List<SettingDef> list = new ArrayList<>();
    IntStream.rangeClosed(1, 10)
        .boxed()
        .map(String::valueOf)
        .forEach(i -> list.add(SettingDef.builder().key(String.valueOf(i)).group(group).build()));
    StreamDefinitions newConfigList = StreamDefinitions.create().addAll(list);
    Assert.assertEquals(
        newConfigList.values().size(), StreamDefinitions.create().values().size() + 10);
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
    StreamDefinitions streamDefinitions = StreamDefinitions.create();
    streamDefinitions
        .add(SettingDef.builder().key("a").group("b").build())
        .add(SettingDef.builder().key("a").group("c").build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDuplicatePartialKey() {
    List<SettingDef> list =
        IntStream.rangeClosed(1, 10)
            .boxed()
            .map(String::valueOf)
            .map(i -> SettingDef.builder().key(String.valueOf(i)).group("b").build())
            .collect(Collectors.toList());

    StreamDefinitions streamDefinitions = StreamDefinitions.create();
    streamDefinitions.add(SettingDef.builder().key("1").group("b").build()).addAll(list);
  }
}
