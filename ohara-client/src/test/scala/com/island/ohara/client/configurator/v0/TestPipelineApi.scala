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

import com.island.ohara.client.configurator.v0.PipelineApi._
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
class TestPipelineApi extends SmallTest with Matchers {

  @Test
  def testDeprecatedRules(): Unit = {
    val rules = Map(
      CommonUtils.randomString() -> Set(CommonUtils.randomString()),
      CommonUtils.randomString() -> Set(CommonUtils.randomString(), CommonUtils.randomString())
    )

    val req = Creation(
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
    val req = PIPELINE_CREATION_JSON_FORMAT.read(s"""
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
    req.flows.head.to shouldBe Set(to0, to1)
  }
  @Test
  def parseDeprecatedJsonOfPipeline(): Unit = {
    val pipeline = Pipeline(
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
      name = CommonUtils.randomString(),
      workerClusterName = CommonUtils.randomString(),
      objects = Seq.empty,
      flows = Seq(
        Flow(
          from = from,
          to = Set(to)
        )
      ),
      lastModified = CommonUtils.current()
    )
    val json = PIPELINE_JSON_FORMAT.write(pipeline).toString
    withClue(json)(json.contains(s"""\"rules\":{\"$from\":[\"$to\"]""") shouldBe true)
  }

  @Test
  def ignoreNameOnCreation(): Unit = an[NullPointerException] should be thrownBy PipelineApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .create()

  @Test
  def ignoreNameOnUpdate(): Unit = an[NullPointerException] should be thrownBy PipelineApi
    .access()
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request()
    .update()

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy PipelineApi.access().request().name("")

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy PipelineApi.access().request().name(null)

  @Test
  def emptyWorkerClusterName(): Unit =
    an[IllegalArgumentException] should be thrownBy PipelineApi.access().request().workerClusterName("")

  @Test
  def nullWorkerClusterName(): Unit =
    an[NullPointerException] should be thrownBy PipelineApi.access().request().workerClusterName(null)

  @Test
  def emptyFlows(): Unit = {
    // pass since the update request requires the empty list
    PipelineApi.access().request().flows(Seq.empty)
  }

  @Test
  def nullFlows(): Unit = an[NullPointerException] should be thrownBy PipelineApi.access().request().flows(null)
}
