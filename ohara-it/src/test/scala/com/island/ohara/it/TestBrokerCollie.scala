package com.island.ohara.it

import org.junit.Test

class TestBrokerCollie extends BasicTestsOfCollie {

  @Test
  def test(): Unit = testBroker { (_, _) => // do nothing since all checks are done
  }
}
