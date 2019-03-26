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

package com.island.ohara.client.configurator.v0

import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorState
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerState
import com.island.ohara.client.configurator.v0.PipelineApi.ObjectState._
import com.island.ohara.client.configurator.v0.PipelineApi._
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
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
      state => OBJECT_STATE_JSON_FORMAT.read(OBJECT_STATE_JSON_FORMAT.write(state)) shouldBe state
    )
  }

  @Test
  def testConvertState(): Unit = {
    ConnectorState.all.foreach(state => ObjectState.forName(state.name))
    ContainerState.all.foreach(state => ObjectState.forName(state.name))
  }

  @Test
  def testDeprecatedRules(): Unit = {
    val rules = Map(
      CommonUtils.randomString() -> Seq(CommonUtils.randomString()),
      CommonUtils.randomString() -> Seq(CommonUtils.randomString(), CommonUtils.randomString())
    )

    val req = PipelineCreationRequest(
      name = CommonUtils.randomString(),
      workerClusterName = None,
      rules = rules
    )
    rules shouldBe req.rules
  }

  @Test
  def parseDeprecatedJsonOfPipelineCreationRequest(): Unit = {
    import spray.json._
    val from = CommonUtils.randomString()
    val to0 = CommonUtils.randomString()
    val to1 = CommonUtils.randomString()
    val req = PIPELINE_REQUEST_JSON_FORMAT.read(s"""
                                               |{
                                               |  "name":"${CommonUtils.randomString()}",
                                               |  "rules": {
                                               |    "$from": [
                                               |      "$to0", "$to1"
                                               |    ]
                                               |  }
                                               |}
                                            """.stripMargin.parseJson)
    req.flows.size shouldBe 1
    req.flows.head.from shouldBe from
    req.flows.head.to.size shouldBe 2
    req.flows.head.to(0) shouldBe to0
    req.flows.head.to(1) shouldBe to1
  }
  @Test
  def parseDeprecatedJsonOfPipeline(): Unit = {
    val pipeline = Pipeline(
      id = CommonUtils.randomString(),
      name = CommonUtils.randomString(),
      workerClusterName = CommonUtils.randomString(),
      objects = Seq.empty,
      flows = Seq.empty,
      lastModified = CommonUtils.current()
    )
    val json = PIPELINE_JSON_FORMAT.write(pipeline).toString
    withClue(json)(json.contains("\"rules\":{") shouldBe true)
  }

  @Test
  def parseDeprecatedJsonOfPipeline2(): Unit = {
    val from = CommonUtils.randomString()
    val to = CommonUtils.randomString()
    val pipeline = Pipeline(
      id = CommonUtils.randomString(),
      name = CommonUtils.randomString(),
      workerClusterName = CommonUtils.randomString(),
      objects = Seq.empty,
      flows = Seq(
        Flow(
          from = from,
          to = Seq(to)
        )
      ),
      lastModified = CommonUtils.current()
    )
    val json = PIPELINE_JSON_FORMAT.write(pipeline).toString
    withClue(json)(json.contains(s"""\"rules\":{\"$from\":[\"$to\"]""") shouldBe true)
  }
}
