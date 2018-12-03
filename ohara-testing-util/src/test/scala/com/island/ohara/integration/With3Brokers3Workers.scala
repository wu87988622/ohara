package com.island.ohara.integration

import com.island.ohara.common.rule.LargeTest
import com.island.ohara.common.util.CloseOnce
import org.junit.{AfterClass, BeforeClass}

/**
  * This class create a mini broker/worker cluster with 3 nodes. And the cluster will be closed after all test cases have been done.
  */
abstract class With3Brokers3Workers extends LargeTest {
  protected def testUtil: OharaTestUtil = With3Brokers3Workers.util
}

object With3Brokers3Workers {
  private var util: OharaTestUtil = _

  @BeforeClass
  def beforeAll(): Unit = {
    util = OharaTestUtil.workers()
  }

  @AfterClass
  def afterAll(): Unit = {
    CloseOnce.close(util)
  }
}
