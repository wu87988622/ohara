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

package com.island.ohara.testing.service;

import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestSshdServer extends SmallTest {

  @Test
  public void testSpecificPort() {
    int port = CommonUtils.availablePort();
    try (SshdServer server = SshdServer.local(port)) {
      Assert.assertEquals(server.port(), port);
    }
  }

  @Test
  public void testRandomPort() {
    try (SshdServer server = SshdServer.local(0, java.util.Collections.emptyList())) {
      Assert.assertNotEquals(server.port(), 0);
    }
  }
}
