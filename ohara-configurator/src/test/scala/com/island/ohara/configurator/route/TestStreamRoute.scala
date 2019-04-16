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

package com.island.ohara.configurator.route

import java.io.File

import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.client.configurator.v0.PipelineApi.{Flow, Pipeline, PipelineCreationRequest}
import com.island.ohara.client.configurator.v0.StreamApi.{StreamListRequest, StreamPropertyRequest}
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterInfo
import com.island.ohara.client.configurator.v0._
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.Configurator
import org.junit.{Before, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestStreamRoute extends SmallTest with Matchers {

  // create all fake cluster
  private[this] val configurator = Configurator.builder().fake().build()

  private[this] val accessStreamList =
    StreamApi.accessOfList().hostname(configurator.hostname).port(configurator.port)
  private[this] val accessStreamProperty =
    StreamApi.accessOfProperty().hostname(configurator.hostname).port(configurator.port)
  private[this] val accessStreamAction =
    StreamApi.accessOfAction().hostname(configurator.hostname).port(configurator.port)
  private[this] val accessTopic =
    TopicApi.access().hostname(configurator.hostname).port(configurator.port)
  private[this] val accessWorker =
    WorkerApi.access().hostname(configurator.hostname).port(configurator.port)
  private[this] val accessPipeline =
    PipelineApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] val fileSize: Int = 3
  private[this] var workerCluster: WorkerClusterInfo = _
  private[this] var pipeline: Pipeline = _

  private[this] def awaitResult[T](f: Future[T]): T = Await.result(f, 20 seconds)

  @Before
  def setUp(): Unit = {
    // create pipeline
    workerCluster = awaitResult(accessWorker.list).head
    val request: PipelineCreationRequest = PipelineCreationRequest(
      name = "test-stream-pipeline",
      workerClusterName = Some(workerCluster.name),
      flows = Seq.empty
    )
    pipeline = awaitResult(accessPipeline.add(request))
  }

  @Test
  def testStreamAppListPage(): Unit = {
    val filePaths = Seq.fill(fileSize) {
      val file = File.createTempFile("empty_", ".jar")
      file.getPath
    }
    // upload files
    awaitResult(accessStreamList.upload(pipeline.id, filePaths)).foreach(jar => {
      jar.jarName.startsWith("empty_") shouldBe true
      jar.name shouldBe "Untitled stream app"
    })

    // get files
    val res = awaitResult(accessStreamList.list(pipeline.id))
    res.foreach(_.name shouldBe "Untitled stream app")
    res.size shouldBe 3

    // delete first jar file
    val deleteStreamJar = res.head
    val d = awaitResult(accessStreamList.delete(deleteStreamJar.id))
    d.name shouldBe "Untitled stream app"
    d.jarName shouldBe deleteStreamJar.jarName
    awaitResult(accessStreamList.list(pipeline.id)).size shouldBe 2

    // update last jar name
    val originJar = res.last
    val newNameJar = StreamListRequest("new-name.jar")
    val updated = awaitResult(accessStreamList.update(originJar.id, newNameJar))
    updated.name shouldBe "Untitled stream app"
    updated.jarName shouldBe "new-name.jar"

    filePaths.foreach(new File(_).deleteOnExit())
  }

  @Test
  def testStreamAppPropertyPage(): Unit = {
    val file = File.createTempFile("empty_", ".jar")

    val jarData = awaitResult(accessStreamList.upload(pipeline.id, Seq(file.getPath)))

    // get properties
    val id = jarData.head.id
    val res1 = awaitResult(accessStreamProperty.get(id))
    // check initial values
    res1.id shouldBe id
    res1.from.size shouldBe 0
    res1.to.size shouldBe 0
    res1.instances shouldBe 0

    // update properties
    val appId = CommonUtils.randomString(5)
    val req = StreamPropertyRequest(appId, Seq("from"), Seq("to"), 1)
    val res2 = awaitResult(accessStreamProperty.update(id, req))
    res2.name shouldBe appId
    res2.from.size shouldBe 1
    res2.to.size shouldBe 1
    res2.instances shouldBe 1

    file.deleteOnExit()
  }

  @Test
  def testStreamAppAction(): Unit = {
    val file = File.createTempFile("empty_", ".jar")
    val instances = 5
    val streamAppName = CommonUtils.assertOnlyNumberAndChar(CommonUtils.randomString(5))

    // upload jar
    val streamJar = awaitResult(accessStreamList.upload(pipeline.id, Seq(file.getPath))).head

    // add topic
    val fromReq = TopicCreationRequest.apply(Some("from"), Some(workerCluster.brokerClusterName), None, None)
    val fromTopic = awaitResult(accessTopic.add(fromReq))
    val toReq = TopicCreationRequest.apply(Some("to"), Some(workerCluster.brokerClusterName), None, None)
    val toTopic = awaitResult(accessTopic.add(toReq))

    // update properties
    val req = StreamPropertyRequest(streamAppName, Seq(fromTopic.id), Seq(toTopic.id), instances)
    awaitResult(accessStreamProperty.update(streamJar.id, req))

    // add streamApp to pipeline
    val pipeLineReq: PipelineCreationRequest = PipelineCreationRequest(
      name = pipeline.id,
      workerClusterName = Some(workerCluster.name),
      // fake flow : from topic -> streamApp -> to topic
      flows = Seq(
        Flow(fromTopic.id, Seq(streamJar.id)),
        Flow(streamJar.id, Seq(toTopic.id)),
      )
    )
    awaitResult(accessPipeline.update(pipeline.id, pipeLineReq))

    val res1 = awaitResult(accessStreamAction.start(streamJar.id))
    res1.id shouldBe streamJar.id
    res1.name shouldBe streamAppName
    res1.pipelineId shouldBe pipeline.id
    res1.from shouldBe Seq(fromTopic.id)
    res1.to shouldBe Seq(toTopic.id)
    res1.jarInfo.name shouldBe streamJar.jarName
    res1.instances shouldBe instances
    res1.state.get shouldBe ContainerState.RUNNING.name

    val res2 = awaitResult(accessStreamAction.status(streamJar.id))
    res2.state.get shouldBe ContainerState.RUNNING.name

    val res3 = awaitResult(accessStreamAction.stop(streamJar.id))
    res3.state shouldBe None

    an[IllegalArgumentException] should be thrownBy
      awaitResult(accessStreamAction.status(streamJar.id))

    file.deleteOnExit()
  }

  @Test
  def testStreamAppListPageFailCases(): Unit = {
    an[IllegalArgumentException] should be thrownBy awaitResult(accessStreamList.delete("fake_id"))

    val newNameJar = StreamListRequest("new-name.jar")
    an[IllegalArgumentException] should be thrownBy awaitResult(accessStreamList.update("fake_id", newNameJar))
    an[NullPointerException] should be thrownBy awaitResult(accessStreamList.update("id", null))
    an[IllegalArgumentException] should be thrownBy
      awaitResult(accessStreamList.update("id", newNameJar.copy("")))
    an[IllegalArgumentException] should be thrownBy
      awaitResult(accessStreamList.update("id", newNameJar.copy(null)))
  }

  @Test
  def testStreamAppActionPageFailCases(): Unit = {
    val file = File.createTempFile("empty_", ".jar")
    val streamAppName = CommonUtils.assertOnlyNumberAndChar(CommonUtils.randomString(5))

    // upload jar
    val streamJar = awaitResult(accessStreamList.upload(pipeline.id, Seq(file.getPath))).head

    // update properties
    val req = StreamPropertyRequest(streamAppName, Seq("topic1_id"), Seq("topic2_id"), 1)

    awaitResult(accessStreamProperty.update(streamJar.id, req.copy(from = Seq.empty)))
    an[IllegalArgumentException] should be thrownBy awaitResult(accessStreamAction.start(streamJar.id))

    awaitResult(accessStreamProperty.update(streamJar.id, req.copy(to = Seq.empty)))
    an[IllegalArgumentException] should be thrownBy awaitResult(accessStreamAction.start(streamJar.id))

    an[NullPointerException] should be thrownBy awaitResult(
      accessStreamProperty.update(streamJar.id, req.copy(from = null)))

    an[NullPointerException] should be thrownBy awaitResult(
      accessStreamProperty.update(streamJar.id, req.copy(to = null)))

    awaitResult(accessStreamProperty.update(streamJar.id, req.copy(instances = 0)))
    an[IllegalArgumentException] should be thrownBy awaitResult(accessStreamAction.start(streamJar.id))

    awaitResult(accessStreamProperty.update(streamJar.id, req.copy(instances = -99)))
    an[IllegalArgumentException] should be thrownBy awaitResult(accessStreamAction.start(streamJar.id))

    file.deleteOnExit()
  }
}
