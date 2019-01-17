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

public class TestZookeepers extends MediumTest {

  @Test
  public void testLocalMethod() throws Exception {
    String url = "localhost:12345";
    try (Zookeepers external = Zookeepers.of(url)) {
      assertFalse(external.isLocal());
      Assert.assertEquals(url, external.connectionProps());
    }

    try (Zookeepers zookeepers = Zookeepers.of()) {
      assertTrue(zookeepers.isLocal());
    }
  }

  @Test
  public void testRandomPort() throws Exception {
    try (Zookeepers zk = Zookeepers.local(0)) {
      Assert.assertNotEquals(0, Integer.parseInt(zk.connectionProps().split(":")[1]));
    }
  }
}
