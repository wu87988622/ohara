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

import com.island.ohara.common.rule.MediumTest;
import org.junit.Assert;
import org.junit.Test;

public class TestFtpServer extends MediumTest {

  @Test(expected = IllegalArgumentException.class)
  public void testErrorFtpConnectionString() {
    // a random string
    FtpServer.of("adadasdasd");
  }

  @Test
  public void testExternalFtpServer() {
    String user = "user";
    String password = "password";
    String host = "host";
    int port = 123;

    FtpServer result = FtpServer.of(user + ":" + password + "@" + host + ":" + port);
    Assert.assertEquals(user, result.user());
    Assert.assertEquals(password, result.password());
    Assert.assertEquals(host, result.hostname());
    Assert.assertEquals(port, result.port());
  }

  @Test
  public void testLocalMethod() throws Exception {
    String user = "user";
    String password = "password";
    String host = "host";
    int port = 123;

    try (FtpServer externalFtpServer =
        FtpServer.of(user + ":" + password + "@" + host + ":" + port); ) {
      assertFalse(externalFtpServer.isLocal());
      Assert.assertEquals(user, externalFtpServer.user());
      Assert.assertEquals(password, externalFtpServer.password());
      Assert.assertEquals(host, externalFtpServer.hostname());
      Assert.assertEquals(port, externalFtpServer.port());
    }

    try (FtpServer localFtpServer = FtpServer.of()) {
      assertTrue(localFtpServer.isLocal());
    }
  }

  @Test
  public void testRandomPort() throws Exception {
    int[] dataPorts = {0};
    try (FtpServer ftpServer = FtpServer.local(0, dataPorts)) {
      Assert.assertNotEquals(0, ftpServer.port());
    }
  }
}
