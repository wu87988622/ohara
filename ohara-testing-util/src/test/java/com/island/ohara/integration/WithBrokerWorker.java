package com.island.ohara.integration;

import com.island.ohara.common.rule.LargeTest;
import com.island.ohara.common.util.CloseOnce;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * This class create a mini broker/worker cluster with single node. And the cluster will be closed
 * after all test cases have been done.
 *
 * <p>NOTED: You can't create a topic with multi-partitions or multi-replication in this env,
 *
 * <p>NOTED: All subclass has "same" reference to util. This is ok in junit test since it default
 * run each test on "different" jvm. The "same" static member won't cause trouble in testing.
 * However, you should move the static "util" into your test if you don't depend on junit...by chia
 */
public abstract class WithBrokerWorker extends LargeTest {
  protected static OharaTestUtil util;

  @BeforeClass
  public static void beforeAll() {
    if (util != null)
      throw new IllegalArgumentException(
          "The test util had been initialized!!! This happens on your tests don't run on different jvm");
    util = OharaTestUtil.worker();
  }

  protected OharaTestUtil testUtil() {
    return util;
  }

  @AfterClass
  public static void afterAll() {
    CloseOnce.close(util);
    // we have to assign null to util since we allow junit to reuse jvm
    util = null;
  }
}
