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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.junit.Test;

public class TestStreamDefUtils extends OharaTest {

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

    StreamSetting.withAll(list);
  }

  @Test
  public void testJarDefinition() {
    Assert.assertEquals(StreamDefUtils.JAR_KEY_DEFINITION.valueType(), SettingDef.Type.OBJECT_KEY);
    Assert.assertEquals(StreamDefUtils.JAR_KEY_DEFINITION.reference(), SettingDef.Reference.FILE);
  }
}
