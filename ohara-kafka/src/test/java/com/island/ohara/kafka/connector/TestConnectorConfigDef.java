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

import com.island.ohara.common.rule.OharaTest;
import com.island.ohara.common.setting.SettingDef;
import com.island.ohara.common.setting.WithDefinitions;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.connector.json.ConnectorDefUtils;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;

public class TestConnectorConfigDef extends OharaTest {

  @Test
  public void testVersion() {
    DumbSink sink = new DumbSink();
    Assert.assertNotNull(sink.config().configKeys().get(WithDefinitions.VERSION_KEY));
  }

  @Test
  public void testRevision() {
    DumbSink sink = new DumbSink();
    Assert.assertNotNull(sink.config().configKeys().get(WithDefinitions.VERSION_KEY));
  }

  @Test
  public void testAuthor() {
    DumbSink sink = new DumbSink();
    Assert.assertNotNull(sink.config().configKeys().get(WithDefinitions.AUTHOR_KEY));
  }

  @Test
  public void testKind() {
    DumbSink sink = new DumbSink();
    Assert.assertNotNull(sink.config().configKeys().get(ConnectorDefUtils.KIND_KEY));
  }

  /** make sure all types from SettingDef are acceptable to kafka type. */
  @Test
  public void testToConfigKey() {
    Stream.of(SettingDef.Type.values())
        .forEach(
            type ->
                ConnectorDefUtils.toConfigKey(
                    SettingDef.builder().key(CommonUtils.randomString()).required(type).build()));
  }
}
