package com.island.ohara.agent

import com.island.ohara.agent.AgentJson.State._
import com.island.ohara.agent.AgentJson._
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestDockerJson extends SmallTest with Matchers {

  @Test
  def testState(): Unit = {
    State.all shouldBe Seq(
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
    State.all.foreach(state =>
      AgentJson.STATE_JSON_FORMAT.read(AgentJson.STATE_JSON_FORMAT.write(state)) shouldBe state)
  }

}
