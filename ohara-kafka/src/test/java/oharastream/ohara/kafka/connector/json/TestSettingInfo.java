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

package oharastream.ohara.kafka.connector.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import oharastream.ohara.common.json.JsonUtils;
import oharastream.ohara.common.rule.OharaTest;
import oharastream.ohara.common.setting.SettingDef;
import oharastream.ohara.common.util.CommonUtils;
import org.apache.kafka.connect.runtime.rest.entities.ConfigInfos;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TestSettingInfo extends OharaTest {
  @Test
  public void testEqual() throws IOException {
    SettingInfo settingInfo =
        SettingInfo.of(
            List.of(
                Setting.of(
                    SettingDef.builder().key(CommonUtils.randomString()).build(),
                    SettingValue.of(
                        CommonUtils.randomString(), CommonUtils.randomString(), List.of()))));
    ObjectMapper mapper = JsonUtils.objectMapper();
    Assert.assertEquals(
        settingInfo,
        mapper.readValue(
            mapper.writeValueAsString(settingInfo), new TypeReference<SettingInfo>() {}));
  }

  @Test
  public void testGetter() {
    Setting setting =
        Setting.of(
            SettingDef.builder().key(CommonUtils.randomString()).build(),
            SettingValue.of(CommonUtils.randomString(), CommonUtils.randomString(), List.of()));
    String name = CommonUtils.randomString();
    SettingInfo settingInfo = SettingInfo.of(List.of(setting));
    Assert.assertEquals(1, settingInfo.settings().size());
    Assert.assertEquals(setting, settingInfo.settings().get(0));
  }

  @Test(expected = NullPointerException.class)
  public void nullSettings() {
    SettingInfo.of((List<Setting>) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptySettings() {
    SettingInfo.of(List.of());
  }

  @Test(expected = NullPointerException.class)
  public void nullConfigInfos() {
    SettingInfo.of((ConfigInfos) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyConfigInfos() {
    ConfigInfos infos = Mockito.mock(ConfigInfos.class);
    Mockito.when(infos.values()).thenReturn(List.of());
    SettingInfo.of(infos);
  }

  @Test
  public void testOfSettings() {
    Setting setting =
        Setting.of(
            SettingDef.builder().key(CommonUtils.randomString()).build(),
            SettingValue.of(
                CommonUtils.randomString(),
                CommonUtils.randomString(),
                List.of(CommonUtils.randomString())));

    Assert.assertEquals(1, SettingInfo.of(List.of(setting)).errorCount());
  }
}
