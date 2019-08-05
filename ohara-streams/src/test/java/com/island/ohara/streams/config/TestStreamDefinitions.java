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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.junit.Test;

public class TestStreamDefinitions extends SmallTest {

  @Test
  public void testConfigJson() {
    StreamDefinitions defaultConfig = StreamDefinitions.DEFAULT;
    StreamDefinitions config = StreamDefinitions.ofJson(defaultConfig.toString());

    Assert.assertEquals(defaultConfig, config);
    Assert.assertEquals(defaultConfig, config);
    Assert.assertEquals(5, defaultConfig.values().size());
    Assert.assertEquals(
        "default config size not equal", config.values().size(), defaultConfig.values().size());

    StreamDefinitions another =
        StreamDefinitions.add(
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
        StreamDefinitions.add(SettingDef.builder().key(key).group(group).build());
    Assert.assertEquals(newConfigs.values().size(), StreamDefinitions.DEFAULT.values().size() + 1);
    Assert.assertTrue(newConfigs.keys().contains(key));

    Map<String, SettingDef> maps = new HashMap<>();
    IntStream.rangeClosed(1, 10)
        .boxed()
        .map(String::valueOf)
        .forEach(
            i -> {
              SettingDef setting = SettingDef.builder().key(String.valueOf(i)).group(group).build();
              maps.putIfAbsent(setting.key(), setting);
            });
    StreamDefinitions newConfigList = StreamDefinitions.addAll(maps);
    Assert.assertEquals(
        newConfigList.values().size(), StreamDefinitions.DEFAULT.values().size() + 10);
    Assert.assertTrue(
        newConfigList
            .keys()
            .containsAll(
                IntStream.rangeClosed(1, 10)
                    .boxed()
                    .map(String::valueOf)
                    .collect(Collectors.toList())));
  }
}
