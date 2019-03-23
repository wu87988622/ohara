/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.client.configurator
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorState
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerState
import com.island.ohara.client.configurator.v0.PipelineApi
import com.island.ohara.client.configurator.v0.PipelineApi.ObjectState
import com.island.ohara.client.configurator.v0.PipelineApi.ObjectState._
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestPipelineApi extends SmallTest with Matchers {

  @Test
  def testState(): Unit = {
    ObjectState.all shouldBe Seq(
      UNKNOWN,
      SUCCEEDED,
      UNASSIGNED,
      RUNNING,
      PAUSED,
      PENDING,
      FAILED,
      CREATED,
      RESTARTING,
      REMOVING,
      EXITED,
      DEAD,
      DESTROYED
    ).sortBy(_.name)
  }

  @Test
  def testStateJson(): Unit = {
    ObjectState.all.foreach(
      state =>
        PipelineApi.OBJECT_STATE_JSON_FORMAT.read(
          PipelineApi.OBJECT_STATE_JSON_FORMAT.write(state)
        ) shouldBe state
    )
  }

  @Test
  def testConvertState(): Unit = {
    ConnectorState.all.foreach(state => ObjectState.forName(state.name))
    ContainerState.all.foreach(state => ObjectState.forName(state.name))
  }
}
