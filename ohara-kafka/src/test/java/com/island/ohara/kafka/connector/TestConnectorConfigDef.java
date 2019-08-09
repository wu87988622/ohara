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

import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.kafka.connector.json.ConnectorDefUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestConnectorConfigDef extends SmallTest {

  @Test
  public void testVersion() {
    DumbSink sink = new DumbSink();
    Assert.assertNotNull(
        sink.config().configKeys().get(ConnectorDefUtils.VERSION_DEFINITION.key()));
  }

  @Test
  public void testRevision() {
    DumbSink sink = new DumbSink();
    Assert.assertNotNull(
        sink.config().configKeys().get(ConnectorDefUtils.REVISION_DEFINITION.key()));
  }

  @Test
  public void testAuthor() {
    DumbSink sink = new DumbSink();
    Assert.assertNotNull(sink.config().configKeys().get(ConnectorDefUtils.AUTHOR_DEFINITION.key()));
  }

  @Test
  public void testKind() {
    DumbSink sink = new DumbSink();
    Assert.assertNotNull(sink.config().configKeys().get(ConnectorDefUtils.KIND_DEFINITION.key()));
  }
}
