package com.island.ohara.it.agent

import org.junit.Test

/**
  * this test invokes 4 test cases in BasicTestsOfCollie.
  * 1) start a single-node zk cluster
  * 2) start a single-node broker cluster
  * 3) start a single-node worker cluster
  * 4) add an new node to the running worker cluster
  * 5) remove a node from the running worker cluster
  */
class TestWorkerCollie extends BasicTestsOfCollie {

  @Test
  def test(): Unit = testRemoveNodeToRunningWorkerCluster()
}
