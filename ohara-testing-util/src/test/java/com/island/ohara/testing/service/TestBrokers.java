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

public class TestBrokers extends MediumTest {

  @Test
  public void testSpecificPort() {
    int port = CommonUtil.availablePort();
    try (Zookeepers zk = Zookeepers.local(0);
        Brokers brokers = Brokers.local(zk, new int[] {port})) {
      assertEquals(port, Integer.parseInt(brokers.connectionProps().split(",")[0].split(":")[1]));
    }
  }

  @Test
  public void testRandomPort() {
    int[] ports = {0};
    try (Zookeepers zk = Zookeepers.local(0);
        Brokers brokers = Brokers.local(zk, ports)) {
      assertNotEquals(0, Integer.parseInt(brokers.connectionProps().split(",")[0].split(":")[1]));
    }
  }
}
