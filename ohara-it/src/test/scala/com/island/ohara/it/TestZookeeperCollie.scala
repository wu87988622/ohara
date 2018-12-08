package com.island.ohara.it
import org.junit.Test

class TestZookeeperCollie extends BasicTestsOfCollie {

  @Test
  def test(): Unit = testZk { (_, _) => // do nothing since all checks are done
  }
}
