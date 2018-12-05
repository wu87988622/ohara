package com.island.ohara.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.island.ohara.common.rule.MediumTest;
import org.junit.Assert;
import org.junit.Test;

public class TestFtpServer extends MediumTest {

  @Test(expected = IllegalArgumentException.class)
  public void testErrorFtpConnectionString() {
    // a random string
    FtpServer.parseString("adadasdasd");
  }

  @Test
  public void testExternalFtpServer() {
    String user = "user";
    String password = "password";
    String host = "host";
    int port = 123;

    FtpServer.FtpServerInfo result =
        FtpServer.parseString(user + ":" + password + "@" + host + ":" + port);
    Assert.assertEquals(user, result.user());
    Assert.assertEquals(password, result.password());
    Assert.assertEquals(host, result.host());
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
