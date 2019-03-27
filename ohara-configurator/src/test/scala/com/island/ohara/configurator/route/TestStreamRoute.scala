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

import com.island.ohara.client.configurator.v0.StreamApi.{StreamListRequest, StreamPropertyRequest}
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.configurator.v0._
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.Configurator
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
class TestStreamRoute extends SmallTest with Matchers {

  // create all fake cluster
  private[this] val configurator = Configurator.builder().fake(1, 1).build()
  private[this] val accessStreamList = StreamApi.accessOfList().hostname(configurator.hostname).port(configurator.port)
  private[this] val accessStreamProperty =
    StreamApi.accessOfProperty().hostname(configurator.hostname).port(configurator.port)
  private[this] val accessStreamAction =
    StreamApi.accessOfAction().hostname(configurator.hostname).port(configurator.port)
  private[this] val accessTopic =
    TopicApi.access().hostname(configurator.hostname).port(configurator.port)
  private[this] val accessWorker =
    WorkerApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] val pipeline_id = "pipeline-id"

  private[this] def awaitResult[T](f: Future[T]): T =
    Await.result(f, 20 seconds)

  @Before
  def tearUp(): Unit = {}

  @Test
  def testStreamAppListPage(): Unit = {
    val filePaths = for (i <- 1 to 3) yield {
      val file = File.createTempFile("empty_", ".jar")
      file.getPath
    }
    // Test POST method
    val res1 = awaitResult(accessStreamList.upload(pipeline_id, filePaths))
    res1.foreach(jar => {
      jar.jarName.startsWith("empty_") shouldBe true
      jar.name shouldBe "Untitled stream app"
    })

    // Test GET method
    val res2 = awaitResult(accessStreamList.list(pipeline_id))
    res2.forall(r => r.name.equals("Untitled stream app")) shouldBe true
    res2.size shouldBe 3

    // Test DELETE method
    val deleteJar = res1.head
    val d = awaitResult(accessStreamList.delete(deleteJar.id))
    d.name shouldBe "Untitled stream app"
    d.jarName shouldBe deleteJar.jarName

    //Test PUT method
    val originJar = res1.last
    val anotherJar = StreamListRequest("la-new.jar")
    val updated = awaitResult(accessStreamList.update(originJar.id, anotherJar))
    updated.name shouldBe "Untitled stream app"
    updated.jarName shouldBe "la-new.jar"

    filePaths.foreach(new File(_).deleteOnExit())
  }

  @Test
  def testStreamAppPropertyPage(): Unit = {
    val filePaths = for (i <- 1 to 3) yield {
      val file = File.createTempFile("empty_", ".jar")
      file.getPath
    }

    val jarData = awaitResult(accessStreamList.upload(pipeline_id, filePaths))

    // Test GET method
    val id = jarData.head.id
    val res1 = awaitResult(accessStreamProperty.get(id))
    res1.id shouldBe id
    res1.fromTopics.size shouldBe 0
    res1.toTopics.size shouldBe 0
    res1.instances shouldBe 1

    // Test PUT method
    val appId = CommonUtils.randomString(5)
    val req =
      StreamPropertyRequest(appId, Seq("from-topic"), Seq("to-topic"), 1)
    val res2 = awaitResult(accessStreamProperty.update(id, req))
    res2.name shouldBe appId
    res2.fromTopics.size shouldBe 1
    res2.toTopics.size shouldBe 1
    res2.instances shouldBe 1

    filePaths.foreach(new File(_).deleteOnExit())
  }

  @Test
  def testStreamAppActionWithWrongParameters(): Unit = {
    val filePath = Seq(File.createTempFile("empty_", ".jar").getPath)

    val jarData = awaitResult(accessStreamList.upload(pipeline_id, filePath))

    val appId = CommonUtils.randomString(5)

    var req = StreamPropertyRequest(appId, Seq("foo"), Seq("bar"), 0)
    accessStreamProperty.update(jarData.head.id, req)
    an[IllegalArgumentException] should be thrownBy awaitResult(
      accessStreamAction.start(jarData.head.id)
    )

    req = StreamPropertyRequest(appId, Seq(""), Seq("bar"), 1)
    awaitResult(accessStreamProperty.update(jarData.head.id, req))
    an[IllegalArgumentException] should be thrownBy awaitResult(
      accessStreamAction.start(jarData.head.id)
    )

    req = StreamPropertyRequest(appId, Seq("foo"), Seq(""), 1)
    awaitResult(accessStreamProperty.update(jarData.head.id, req))
    an[IllegalArgumentException] should be thrownBy awaitResult(
      accessStreamAction.start(jarData.head.id)
    )

    req = StreamPropertyRequest("", Seq("foo"), Seq("bar"), 1)
    awaitResult(accessStreamProperty.update(jarData.head.id, req))
    an[IllegalArgumentException] should be thrownBy awaitResult(
      accessStreamAction.start(jarData.head.id)
    )

    req = StreamPropertyRequest(
      "any-name contain blank should still work",
      Seq("foo"),
      Seq("bar"),
      1
    )
    awaitResult(accessStreamProperty.update(jarData.head.id, req))
    an[IllegalArgumentException] should be thrownBy awaitResult(
      accessStreamAction.start(jarData.head.id)
    )

    filePath.foreach(new File(_).deleteOnExit())
  }

  @Test
  def testStreamRunFailWithFakeNode(): Unit = {
    val fromTopic = "foo"
    val toTopic = "bar"

    //Upload file
    val filePath = Seq(File.createTempFile("empty_", ".jar").getPath)
    val jarData = awaitResult(accessStreamList.upload(pipeline_id, filePath))

    //Get Worker Cluster
    val clusters = awaitResult(accessWorker.list)
    clusters.size shouldBe 1
    val bkCluster = clusters.head.brokerClusterName

    //create topic
    val req1 = TopicCreationRequest.apply(Some(fromTopic), Some(bkCluster), None, None)
    val from = awaitResult(accessTopic.add(req1)).id
    val req2 = TopicCreationRequest.apply(Some(toTopic), Some(bkCluster), None, None)
    val to = awaitResult(accessTopic.add(req2)).id

    val appId = CommonUtils.randomString(5)
    val req = StreamPropertyRequest(appId, Seq(from), Seq(to), 1)
    awaitResult(accessStreamProperty.update(jarData.head.id, req))
    val res = awaitResult(accessStreamAction.start(jarData.head.id))
    res.state.nonEmpty shouldBe true
    res.state.get shouldBe ContainerApi.ContainerState.EXITED.name

    // idempotent
    val res2 = awaitResult(accessStreamAction.start(jarData.head.id))
    res2.state.nonEmpty shouldBe true
    res2.state.get shouldBe ContainerApi.ContainerState.EXITED.name

    filePath.foreach(new File(_).deleteOnExit())
  }

  @After
  def tearDown(): Unit = {}
}
