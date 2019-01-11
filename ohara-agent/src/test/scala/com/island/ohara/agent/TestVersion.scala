package com.island.ohara.agent
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.VersionUtil
import org.junit.Test
import org.scalatest.Matchers

class TestVersion extends SmallTest with Matchers {

  @Test
  def testZookeeper(): Unit = {
    ZookeeperCollie.IMAGE_NAME_DEFAULT shouldBe s"oharastream/zookeeper:${VersionUtil.VERSION}"
  }

  @Test
  def testBroker(): Unit = {
    BrokerCollie.IMAGE_NAME_DEFAULT shouldBe s"oharastream/broker:${VersionUtil.VERSION}"
  }

  @Test
  def testWorker(): Unit = {
    WorkerCollie.IMAGE_NAME_DEFAULT shouldBe s"oharastream/connect-worker:${VersionUtil.VERSION}"
  }
}
