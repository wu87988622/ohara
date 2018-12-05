package com.island.ohara.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
