package com.island.ohara.integration;

import com.island.ohara.common.rule.MediumTest;
import org.junit.Assert;
import org.junit.Test;

public class TestBrokers extends MediumTest {

  @Test(expected = IllegalArgumentException.class)
  public void testBrokersNull() {
    Brokers.of(
        null,
        () -> {
          throw new IllegalArgumentException("you can't pass");
        },
        1);
  }

  @Test
  public void testHaveConnProps() throws Exception {
    String connProps = "localhost:12345";
    try (Brokers external =
        Brokers.of(
            connProps,
            () -> {
              throw new IllegalArgumentException("you can't pass");
            },
            1)) {

      Assert.assertEquals(connProps, external.connectionProps());
      assertFalse(external.isLocal());
    }
  }

  @Test
  public void testLocalMethod() throws Exception {
    try (Zookeepers zk = Zookeepers.of();
        Brokers local = Brokers.of(() -> zk, 1)) {
      assertTrue(local.isLocal());
    }
  }

  @Test
  public void testRandomPort() throws Exception {
    int[] ports = {0};
    try (Zookeepers zk = Zookeepers.local(0);
        Brokers brokers = Brokers.local(zk, ports)) {
      Assert.assertNotEquals(
          0, Integer.parseInt(brokers.connectionProps().split(",")[0].split(":")[1]));
    }
  }
}
