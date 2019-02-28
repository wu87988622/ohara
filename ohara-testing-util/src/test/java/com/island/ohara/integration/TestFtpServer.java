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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.island.ohara.common.rule.MediumTest;
import com.island.ohara.common.util.CommonUtil;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;

public class TestFtpServer extends MediumTest {

  @Test(expected = NullPointerException.class)
  public void nullUser() {
    FtpServer.builder().user(null).build();
  }

  @Test(expected = NullPointerException.class)
  public void nullPassword() {
    FtpServer.builder().password(null).build();
  }

  @Test(expected = NullPointerException.class)
  public void nullDataPorts() {
    FtpServer.builder().dataPorts(null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyDataPorts() {
    FtpServer.builder().dataPorts(Collections.emptyList()).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeControlPort() {
    FtpServer.builder().controlPort(-1).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeDataPort() {
    FtpServer.builder().dataPorts(Collections.singletonList(-1)).build();
  }

  @Test(expected = NullPointerException.class)
  public void nullAdvertisedHostname() {
    FtpServer.builder().advertisedHostname(null).build();
  }

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
    assertEquals(user, result.user());
    assertEquals(password, result.password());
    assertEquals(host, result.hostname());
    assertEquals(port, result.port());
  }

  @Test
  public void testLocalMethod() {
    String user = "user";
    String password = "password";
    String host = "host";
    int port = 123;

    try (FtpServer externalFtpServer =
        FtpServer.of(user + ":" + password + "@" + host + ":" + port); ) {
      assertFalse(externalFtpServer.isLocal());
      assertEquals(user, externalFtpServer.user());
      assertEquals(password, externalFtpServer.password());
      assertEquals(host, externalFtpServer.hostname());
      assertEquals(port, externalFtpServer.port());
    }

    try (FtpServer localFtpServer = FtpServer.of()) {
      assertTrue(localFtpServer.isLocal());
    }
  }

  @Test
  public void testRandomPort() {
    List<Integer> dataPorts = Collections.singletonList(0);
    try (FtpServer ftpServer = FtpServer.builder().controlPort(0).dataPorts(dataPorts).build()) {
      assertNotEquals(0, ftpServer.port());
    }
  }

  @Test
  public void testTtl() throws InterruptedException {
    int ttl = 3;
    ExecutorService es = Executors.newSingleThreadExecutor();
    try {
      es.execute(
          () -> {
            try {
              FtpServer.start(
                  new String[] {
                    FtpServer.CONTROL_PORT, String.valueOf(CommonUtil.availablePort()),
                    FtpServer.DATA_PORTS, String.valueOf(CommonUtil.availablePort()),
                    FtpServer.TTL, String.valueOf(ttl)
                  },
                  ftp -> {});
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          });
    } finally {
      es.shutdown();
      assertTrue(es.awaitTermination(ttl * 2, TimeUnit.SECONDS));
    }
  }

  @Test
  public void defaultMain() throws InterruptedException {
    ExecutorService es = Executors.newSingleThreadExecutor();
    try {
      CountDownLatch latch = new CountDownLatch(1);
      es.execute(
          () -> {
            try {
              FtpServer.start(
                  new String[0],
                  ftp -> {
                    latch.countDown();
                  });
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          });
      assertTrue(latch.await(10, TimeUnit.SECONDS));
    } finally {
      es.shutdownNow();
    }
  }

  @Test
  public void testInputs() throws InterruptedException {
    String user = CommonUtil.randomString(5);
    String password = CommonUtil.randomString(5);
    int controlPort = CommonUtil.availablePort();
    List<Integer> dataPorts =
        IntStream.range(0, 3)
            .map(i -> CommonUtil.availablePort())
            .boxed()
            .collect(Collectors.toList());
    int ttl = 3;
    ExecutorService es = Executors.newSingleThreadExecutor();
    try {
      es.execute(
          () -> {
            try {
              FtpServer.start(
                  new String[] {
                    FtpServer.USER, user,
                    FtpServer.PASSWORD, password,
                    FtpServer.CONTROL_PORT, String.valueOf(controlPort),
                    FtpServer.DATA_PORTS,
                        dataPorts.stream().map(String::valueOf).collect(Collectors.joining(",")),
                    FtpServer.TTL, String.valueOf(ttl)
                  },
                  ftp -> {
                    assertEquals(ftp.user(), user);
                    assertEquals(ftp.password(), password);
                    assertEquals(ftp.port(), controlPort);
                    assertEquals(ftp.dataPorts(), dataPorts);
                  });
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          });
    } finally {
      es.shutdown();
      assertTrue(es.awaitTermination(ttl * 2, TimeUnit.SECONDS));
    }
  }

  @Test
  public void testInputs2() throws InterruptedException {
    String user = CommonUtil.randomString(5);
    String password = CommonUtil.randomString(5);
    int controlPort = CommonUtil.availablePort();
    int portRange = 2;
    int p = CommonUtil.availablePort();
    List<Integer> dataPorts =
        IntStream.range(p, p + portRange).boxed().collect(Collectors.toList());
    int ttl = 3;
    ExecutorService es = Executors.newSingleThreadExecutor();
    try {
      es.execute(
          () -> {
            try {
              FtpServer.start(
                  new String[] {
                    FtpServer.USER,
                    user,
                    FtpServer.PASSWORD,
                    password,
                    FtpServer.CONTROL_PORT,
                    String.valueOf(controlPort),
                    FtpServer.DATA_PORTS,
                    p + "-" + (p + portRange),
                    FtpServer.TTL,
                    String.valueOf(ttl)
                  },
                  ftp -> {
                    assertEquals(ftp.user(), user);
                    assertEquals(ftp.password(), password);
                    assertEquals(ftp.port(), controlPort);
                    assertEquals(ftp.dataPorts(), dataPorts);
                  });
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          });
    } finally {
      es.shutdown();
      assertTrue(es.awaitTermination(ttl * 2, TimeUnit.SECONDS));
    }
  }
}
