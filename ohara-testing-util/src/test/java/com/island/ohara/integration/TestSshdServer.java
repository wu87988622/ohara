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

package com.island.ohara.integration;

import com.island.ohara.common.rule.SmallTest;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class TestSshdServer extends SmallTest {
  @Test
  public void testParse() {
    String user = "aaa";
    String password = "ccc";
    String hostname = "aaaaa";
    int port = 12345;
    List<String> items =
        SshdServer.parseString(user + ":" + password + "@" + hostname + ":" + port);
    Assert.assertEquals(items.get(0), user);
    Assert.assertEquals(items.get(1), password);
    Assert.assertEquals(items.get(2), hostname);
    Assert.assertEquals(Integer.parseInt(items.get(3)), port);
  }

  @Test
  public void testOf() {
    String user = "aaa";
    String password = "ccc";
    String hostname = "aaaaa";
    int port = 12345;
    try (SshdServer server = SshdServer.of(user + ":" + password + "@" + hostname + ":" + port)) {
      Assert.assertEquals(server.user(), user);
      Assert.assertEquals(server.password(), password);
      Assert.assertEquals(server.hostname(), hostname);
      Assert.assertEquals(server.port(), port);
    }
  }

  @Test
  public void testPort() {
    try (SshdServer server = SshdServer.local(0, java.util.Collections.emptyList())) {
      Assert.assertNotEquals(server.port(), 0);
    }
  }
}
