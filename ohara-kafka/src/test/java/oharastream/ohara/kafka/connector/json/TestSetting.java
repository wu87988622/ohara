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
import java.util.Collections;
import oharastream.ohara.common.json.JsonUtils;
import oharastream.ohara.common.rule.OharaTest;
import oharastream.ohara.common.setting.SettingDef;
import oharastream.ohara.common.util.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestSetting extends OharaTest {
  @Test
  public void testEqual() throws IOException {
    Setting config =
        Setting.of(
            SettingDef.builder().key(CommonUtils.randomString()).build(),
            SettingValue.of(
                CommonUtils.randomString(), CommonUtils.randomString(), Collections.emptyList()));
    ObjectMapper mapper = JsonUtils.objectMapper();
    Assert.assertEquals(
        config,
        mapper.readValue(mapper.writeValueAsString(config), new TypeReference<Setting>() {}));
  }

  @Test
  public void testGetter() {
    SettingDef def = SettingDef.builder().key(CommonUtils.randomString()).build();
    SettingValue value =
        SettingValue.of(
            CommonUtils.randomString(), CommonUtils.randomString(), Collections.emptyList());
    Setting config = Setting.of(def, value);
    Assert.assertEquals(def, config.definition());
    Assert.assertEquals(value, config.value());
  }

  @Test(expected = NullPointerException.class)
  public void nullDefinition() {
    Setting.of(
        null,
        SettingValue.of(
            CommonUtils.randomString(), CommonUtils.randomString(), Collections.emptyList()));
  }

  @Test(expected = NullPointerException.class)
  public void nullValue() {
    Setting.of(SettingDef.builder().key(CommonUtils.randomString()).build(), null);
  }
}
