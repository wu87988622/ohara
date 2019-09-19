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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.island.ohara.common.rule.OharaTest;
import com.island.ohara.common.util.CommonUtils;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;

public class TestFtpServer extends OharaTest {

  @Test(expected = NullPointerException.class)
  public void nullUser() {
    FtpServer.builder().user(null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyUser() {
    FtpServer.builder().user("").build();
  }

  @Test(expected = NullPointerException.class)
  public void nullPassword() {
    FtpServer.builder().password(null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyPassword() {
    FtpServer.builder().password("").build();
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

  @Test(expected = NullPointerException.class)
  public void nullHomeFolder() {
    FtpServer.builder().homeFolder(null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void setFileToHomeFolder() {
    File f = CommonUtils.createTempJar(CommonUtils.randomString(5));
    FtpServer.builder().homeFolder(f).build();
  }

  @Test
  public void testSpecificDataPort() {
    int port = CommonUtils.availablePort();
    try (FtpServer ftpServer =
        FtpServer.builder().dataPorts(Collections.singletonList(port)).build()) {
      assertEquals(port, (int) ftpServer.dataPorts().get(0));
    }
  }

  @Test
  public void testRandomDataPort() {
    try (FtpServer ftpServer = FtpServer.builder().build()) {
      assertFalse(ftpServer.dataPorts().isEmpty());
      ftpServer.dataPorts().forEach(p -> assertNotEquals(0, (long) p));
    }
  }

  @Test
  public void testSpecificControlPort() {
    int port = CommonUtils.availablePort();
    try (FtpServer ftpServer = FtpServer.builder().controlPort(port).build()) {
      assertEquals(port, ftpServer.port());
    }
  }

  @Test
  public void testRandomControlPort() {
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
                    FtpServer.CONTROL_PORT, String.valueOf(CommonUtils.availablePort()),
                    FtpServer.DATA_PORTS, String.valueOf(CommonUtils.availablePort()),
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
    String user = CommonUtils.randomString(5);
    String password = CommonUtils.randomString(5);
    int controlPort = CommonUtils.availablePort();
    List<Integer> dataPorts =
        IntStream.range(0, 3)
            .map(i -> CommonUtils.availablePort())
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
    String user = CommonUtils.randomString(5);
    String password = CommonUtils.randomString(5);
    int controlPort = CommonUtils.availablePort();
    int portRange = 2;
    int p = CommonUtils.availablePort();
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

  @Test
  public void testHomeFolder() {
    String prefix = CommonUtils.randomString(5);
    File f = CommonUtils.createTempFolder(prefix);
    assertTrue(f.delete());
    assertFalse(f.exists());
    try (FtpServer ftpServer = FtpServer.builder().homeFolder(f).build()) {
      assertTrue(ftpServer.isLocal());
      assertTrue(f.exists());
      assertEquals(ftpServer.absolutePath(), f.getAbsolutePath());
    }
  }

  @Test
  public void testPortOfLocal() {
    try (FtpServer ftpServer = FtpServer.local()) {
      assertNotEquals(0, ftpServer.port());
    }
  }
}
