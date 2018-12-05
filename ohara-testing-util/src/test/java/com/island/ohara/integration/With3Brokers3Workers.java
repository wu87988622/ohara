package com.island.ohara.integration;

import com.island.ohara.common.rule.LargeTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * This class create a mini broker/worker cluster with 3 nodes. And the cluster will be closed after
 * all test cases have been done.
 */
public abstract class With3Brokers3Workers extends LargeTest {
  protected static OharaTestUtil util;

  @BeforeClass
  public static void beforeAll() {
    util = OharaTestUtil.workers();
  }

  protected OharaTestUtil testUtil() {
    return util;
  }

  @AfterClass
  public static void afterAll() throws Exception {
    util.close();
  }
}
