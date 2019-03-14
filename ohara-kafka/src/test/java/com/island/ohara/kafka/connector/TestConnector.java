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

import static com.island.ohara.kafka.connector.ConnectorUtils.VERSION;
import static org.junit.Assert.assertEquals;

import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.VersionUtil;
import org.junit.Test;

public class TestConnector extends SmallTest {

  /**
   * this test is used to prevent us from breaking the format from version exposed to kafka
   * connector
   */
  @Test
  public void testVersion() {
    assertEquals(VERSION, VersionUtil.VERSION + "_" + VersionUtil.REVISION);
  }
}
