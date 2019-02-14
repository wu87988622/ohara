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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class TestDatabase extends MediumTest {

  @Test(expected = IllegalArgumentException.class)
  public void testErrorConnectionString() {
    String dbInstance = "mysql";
    String user = "user";
    String password = "password";
    String host = "host";
    int port = 123;
    String dbName = "dbName";

    // the string should start with "jdbc"
    Database.of(
        "abc:"
            + dbInstance
            + ":"
            + user
            + ":"
            + password
            + "@//"
            + host
            + ":"
            + port
            + "/"
            + dbName);

    // a random string
    Database.of("adadasdasd");
  }

  @Test
  public void testExternalDb() {
    String dbInstance = "mysql";
    String user = "user";
    String password = "password";
    String host = "host";
    int port = 123;
    String dbName = "dbName";

    Database result =
        Database.of(
            "jdbc:"
                + dbInstance
                + ":"
                + user
                + ":"
                + password
                + "@//"
                + host
                + ":"
                + port
                + "/"
                + dbName);

    assertEquals(user, result.user());
    assertEquals(password, result.password());
    assertEquals(host, result.hostname());
    assertEquals(port, result.port());
    assertEquals(dbName, result.databaseName());
  }

  @Test
  public void testLocalMethod() throws Exception {
    String dbInstance = "mysql";
    String user = "user";
    String password = "password";
    String host = "host";
    int port = 123;
    String dbName = "dbName";

    String dbConnectionString =
        "jdbc:"
            + dbInstance
            + ":"
            + user
            + ":"
            + password
            + "@//"
            + host
            + ":"
            + port
            + "/"
            + dbName;
    try (Database externaldb = Database.of(dbConnectionString)) {
      assertFalse(externaldb.isLocal());
      assertEquals(user, externaldb.user());
      assertEquals(password, externaldb.password());
      assertEquals(host, externaldb.hostname());
      assertEquals(port, externaldb.port());
      assertEquals(dbName, externaldb.databaseName());
    }

    try (Database localdb = Database.of()) {
      assertTrue(localdb.isLocal());
    }
  }

  @Test
  public void testRandomPort() {
    try (Database db = Database.local(0)) {
      assertNotEquals(0, db.port());
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
              Database.start(
                  new String[] {
                    Database.PORT, String.valueOf(CommonUtil.availablePort()),
                    FtpServer.TTL, String.valueOf(ttl)
                  },
                  mysql -> {});
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          });
    } finally {
      es.shutdown();
      assertTrue(es.awaitTermination(ttl * 10, TimeUnit.SECONDS));
    }
  }

  @Test(expected = NullPointerException.class)
  public void failWithoutPort() throws InterruptedException {
    FtpServer.start(new String[] {}, mysql -> {});
  }

  @Test
  public void testInputs() throws InterruptedException {
    String user = CommonUtil.randomString(5);
    String password = CommonUtil.randomString(5);
    String dbName = CommonUtil.randomString(5);
    int port = CommonUtil.availablePort();
    int ttl = 3;
    ExecutorService es = Executors.newSingleThreadExecutor();
    try {
      es.execute(
          () -> {
            try {
              Database.start(
                  new String[] {
                    Database.USER, user,
                    Database.PASSWORD, password,
                    Database.PORT, String.valueOf(port),
                    Database.DB_NAME, dbName,
                    FtpServer.TTL, String.valueOf(ttl)
                  },
                  mysql -> {
                    assertEquals(mysql.user(), user);
                    assertEquals(mysql.password(), password);
                    assertEquals(mysql.port(), port);
                    assertEquals(mysql.databaseName(), dbName);
                  });
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          });
    } finally {
      es.shutdown();
      assertTrue(es.awaitTermination(ttl * 10, TimeUnit.SECONDS));
    }
  }
}
