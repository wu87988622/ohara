package com.island.ohara.it.agent

import org.junit.Test

/**
  * this test invokes 4 test cases in BasicTestsOfCollie.
  * 1) start a single-node zk cluster
  * 2) start a single-node broker cluster
  * 3) add an new node to the running broker cluster
  * 4) remove a node from the running broker cluster
  */
class TestBrokerCollie extends BasicTestsOfCollie {

  @Test
  def test(): Unit = testRemoveNodeToRunningBrokerCluster()
}
