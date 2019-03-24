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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.apache.kafka.connect.runtime.rest.entities.ConfigInfos;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TestSettingInfo extends SmallTest {
  @Test
  public void testEqual() throws IOException {
    SettingInfo settingInfo =
        SettingInfo.of(
            Collections.singletonList(
                Setting.of(
                    SettingDefinition.builder().key(CommonUtils.randomString()).build(),
                    SettingValue.of(
                        CommonUtils.randomString(),
                        CommonUtils.randomString(),
                        Collections.emptyList()))));
    ObjectMapper mapper = new ObjectMapper();
    Assert.assertEquals(
        settingInfo,
        mapper.readValue(
            mapper.writeValueAsString(settingInfo), new TypeReference<SettingInfo>() {}));
  }

  @Test
  public void testGetter() {
    Setting setting =
        Setting.of(
            SettingDefinition.builder().key(CommonUtils.randomString()).build(),
            SettingValue.of(
                CommonUtils.randomString(), CommonUtils.randomString(), Collections.emptyList()));
    String name = CommonUtils.randomString();
    SettingInfo settingInfo = SettingInfo.of(Collections.singletonList(setting));
    Assert.assertEquals(1, settingInfo.settings().size());
    Assert.assertEquals(setting, settingInfo.settings().get(0));
  }

  @Test(expected = NullPointerException.class)
  public void nullSettings() {
    SettingInfo.of((List<Setting>) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptySettings() {
    SettingInfo.of(Collections.emptyList());
  }

  @Test(expected = NullPointerException.class)
  public void nullConfigInfos() {
    SettingInfo.of((ConfigInfos) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyConfigInfos() {
    ConfigInfos infos = Mockito.mock(ConfigInfos.class);
    Mockito.when(infos.values()).thenReturn(Collections.emptyList());
    SettingInfo.of(infos);
  }

  @Test
  public void testOfSettings() {
    Setting setting =
        Setting.of(
            SettingDefinition.builder().key(CommonUtils.randomString()).build(),
            SettingValue.of(
                CommonUtils.randomString(),
                CommonUtils.randomString(),
                Collections.singletonList(CommonUtils.randomString())));

    Assert.assertEquals(1, SettingInfo.of(Collections.singletonList(setting)).errorCount());
  }
}
