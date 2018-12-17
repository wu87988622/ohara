package com.island.ohara.kafka.connector;

import static com.island.ohara.kafka.connector.ConnectorUtil.VERSION;

import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.VersionUtil;
import org.junit.Test;

public class TestConnector extends SmallTest {

  /**
   * this test is used to prevent us from breaking the format from version exposed to kafka
   * connector
   */
  @Test
  public void testVersion() {
    assertEquals(VERSION, VersionUtil.VERSION + "_" + VersionUtil.REVISION);
  }
}
