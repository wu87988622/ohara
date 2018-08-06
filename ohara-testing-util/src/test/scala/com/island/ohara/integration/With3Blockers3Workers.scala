package com.island.ohara.integration

import com.island.ohara.io.CloseOnce.close
import com.island.ohara.rule.LargeTest
import org.junit.{AfterClass, BeforeClass}

/**
  * This class create a mini broker/worker cluster with 3 nodes. And the cluster will be closed after all test cases have been done.
  */
abstract class With3Blockers3Workers extends LargeTest {
  protected def testUtil = With3Blockers3Workers.util
}

object With3Blockers3Workers {
  private var util: OharaTestUtil = _

  @BeforeClass
  def beforeAll(): Unit = {
    util = OharaTestUtil.localWorkers(3, 3)
  }

  @AfterClass
  def afterAll(): Unit = {
    close(util)
  }
}
