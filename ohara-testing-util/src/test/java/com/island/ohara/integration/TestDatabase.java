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

    Assert.assertEquals(user, result.user());
    Assert.assertEquals(password, result.password());
    Assert.assertEquals(host, result.hostname());
    Assert.assertEquals(port, result.port());
    Assert.assertEquals(dbName, result.databaseName());
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
      Assert.assertEquals(user, externaldb.user());
      Assert.assertEquals(password, externaldb.password());
      Assert.assertEquals(host, externaldb.hostname());
      Assert.assertEquals(port, externaldb.port());
      Assert.assertEquals(dbName, externaldb.databaseName());
    }

    try (Database localdb = Database.of()) {
      assertTrue(localdb.isLocal());
    }
  }

  @Test
  public void testRandomPort() throws Exception {
    try (Database db = Database.local(0)) {
      Assert.assertNotEquals(0, db.port());
    }
  }
}
