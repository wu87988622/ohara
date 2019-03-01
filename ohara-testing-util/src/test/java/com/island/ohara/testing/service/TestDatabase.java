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
import static org.junit.Assert.assertNotEquals;

import com.island.ohara.common.rule.MediumTest;
import com.island.ohara.common.util.CommonUtil;
import org.junit.Test;

public class TestDatabase extends MediumTest {

  @Test(expected = NullPointerException.class)
  public void nullUser() {
    Database.builder().user(null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyUser() {
    Database.builder().user("").build();
  }

  @Test(expected = NullPointerException.class)
  public void nullPassword() {
    Database.builder().password(null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyPassword() {
    Database.builder().password("").build();
  }

  @Test(expected = NullPointerException.class)
  public void nullDatabaseName() {
    Database.builder().databaseName(null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyDatabase() {
    Database.builder().databaseName("").build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeControlPort() {
    Database.builder().port(-1).build();
  }

  @Test
  public void testSpecificPort() {
    int port = CommonUtil.availablePort();
    try (Database db = Database.builder().port(port).build()) {
      assertEquals(port, db.port());
    }
  }

  @Test
  public void testRandomPort() {
    try (Database db = Database.builder().build()) {
      assertNotEquals(0, db.port());
    }
  }

  @Test
  public void testPortOfLocal() {
    try (Database db = Database.local()) {
      assertNotEquals(0, db.port());
    }
  }
}
