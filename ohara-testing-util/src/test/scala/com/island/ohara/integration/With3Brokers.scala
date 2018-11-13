package com.island.ohara.integration

import com.island.ohara.client.util.CloseOnce.close
import com.island.ohara.common.rule.LargeTest
import org.junit.{AfterClass, BeforeClass}

/**
  * This class create a mini broker cluster with 3 nodes. And the cluster will be closed after all test cases have been done.
  */
abstract class With3Brokers extends LargeTest {
  protected def testUtil: OharaTestUtil = With3Brokers.util
}

object With3Brokers {
  private var util: OharaTestUtil = _

  @BeforeClass
  def beforeAll(): Unit = {
    util = OharaTestUtil.brokers()
  }

  @AfterClass
  def afterAll(): Unit = {
    close(util)
  }
}
