package com.island.ohara.agent

import com.island.ohara.client.ConfiguratorJson
import com.island.ohara.client.ConfiguratorJson.ContainerState
import com.island.ohara.client.ConfiguratorJson.ContainerState._
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestDockerJson extends SmallTest with Matchers {

  @Test
  def testState(): Unit = {
    ContainerState.all shouldBe Seq(
      CREATED,
      RESTARTING,
      RUNNING,
      REMOVING,
      PAUSED,
      EXITED,
      DEAD
    )
  }

  @Test
  def testStateJson(): Unit = {
    ContainerState.all.foreach(
      state =>
        ConfiguratorJson.CONTAINER_STATE_JSON_FORMAT
          .read(ConfiguratorJson.CONTAINER_STATE_JSON_FORMAT.write(state)) shouldBe state)
  }

}
