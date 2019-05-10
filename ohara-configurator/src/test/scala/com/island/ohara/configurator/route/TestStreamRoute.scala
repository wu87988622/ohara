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
import com.island.ohara.client.configurator.v0.StreamApi.{StreamListRequest, StreamPropertyRequest}
import com.island.ohara.client.configurator.v0._
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global

class TestStreamRoute extends SmallTest with Matchers {

  // create all fake cluster
  private[this] val configurator = Configurator.builder().fake().build()

  private[this] val accessStreamList =
    StreamApi.accessOfList().hostname(configurator.hostname).port(configurator.port)
  private[this] val accessStreamProperty =
    StreamApi.accessOfProperty().hostname(configurator.hostname).port(configurator.port)
  private[this] val accessStreamAction =
    StreamApi.accessOfAction().hostname(configurator.hostname).port(configurator.port)

  private[this] val fileSize: Int = 5

  @Test
  def testStreamAppListPage(): Unit = {
    val filePaths = Seq.fill(fileSize) {
      val file = File.createTempFile("empty_", ".jar")
      file.getPath
    }
    // upload files (without assign wkName, will upload to pre-defined worker cluster)
    result(accessStreamList.upload(filePaths, None)).foreach(jar => {
      jar.jarName.startsWith("empty_") shouldBe true
      jar.name shouldBe "Untitled stream app"
    })

    // get all files
    val res = result(accessStreamList.list(None))
    res.foreach(_.name shouldBe "Untitled stream app")
    res.size shouldBe fileSize

    // get files with not exists cluster
    result(accessStreamList.list(Some("non_exist_worker_cluster"))).size shouldBe 0

    // delete first jar file
    val deleteStreamJar = res.head
    result(accessStreamList.delete(deleteStreamJar.id))
    result(accessStreamList.list(None)).size shouldBe fileSize - 1

    // update last jar name
    val originJar = res.last
    val newNameJar = StreamListRequest("new-name.jar")
    val updated = result(accessStreamList.update(originJar.id, newNameJar))
    updated.name shouldBe "Untitled stream app"
    updated.jarName shouldBe "new-name.jar"

    val wkName = result(configurator.clusterCollie.workerCollie().clusters).keys.head.name
    // upload same files but specific worker cluster
    result(accessStreamList.upload(filePaths, Some(wkName))).foreach(jar => {
      jar.jarName.startsWith("empty_") shouldBe true
      jar.name shouldBe "Untitled stream app"
    })
    // upload twice to same cluster (fileSize * 2), and delete one jar (-1)
    result(accessStreamList.list(Some(wkName))).size shouldBe fileSize * 2 - 1
    // without parameter, same result as previous
    result(accessStreamList.list(None)).size shouldBe fileSize * 2 - 1

    filePaths.foreach(new File(_).deleteOnExit())
  }

  @Test
  def testStreamAppPropertyPage(): Unit = {
    val file = File.createTempFile("empty_", ".jar")

    val jarData = result(accessStreamList.upload(Seq(file.getPath), None))

    // get properties
    val id = jarData.head.id
    val res1 = result(accessStreamProperty.get(id))
    // check initial values
    res1.id shouldBe id
    res1.from.size shouldBe 0
    res1.to.size shouldBe 0
    res1.instances shouldBe 0

    // update properties
    val appId = CommonUtils.randomString(5)
    val req = StreamPropertyRequest(appId, Seq("from"), Seq("to"), 1)
    val res2 = result(accessStreamProperty.update(id, req))
    res2.name shouldBe appId
    res2.from.size shouldBe 1
    res2.to.size shouldBe 1
    res2.instances shouldBe 1

    // delete properties
    result(accessStreamProperty.delete(id))

    // after delete, the streamApp should not exist
    an[IllegalArgumentException] should be thrownBy result(accessStreamProperty.get(id))

    file.deleteOnExit()
  }

  @Test
  def testStreamAppAction(): Unit = {
    val file = File.createTempFile("empty_", ".jar")
    val instances = 3
    val streamAppName = CommonUtils.assertOnlyNumberAndChar(CommonUtils.randomString(5))

    // upload jar
    val streamJar = result(accessStreamList.upload(Seq(file.getPath), None)).head

    // update properties
    val req = StreamPropertyRequest(streamAppName, Seq("fromTopic_id"), Seq("toTopic_id"), instances)
    result(accessStreamProperty.update(streamJar.id, req))

    val res1 = result(accessStreamAction.start(streamJar.id))
    res1.id shouldBe streamJar.id
    res1.name shouldBe streamAppName
    res1.workerClusterName should not be None
    res1.from shouldBe Seq("fromTopic_id")
    res1.to shouldBe Seq("toTopic_id")
    res1.jarInfo.name shouldBe streamJar.jarName
    res1.instances shouldBe instances
    res1.state.get shouldBe ContainerState.RUNNING.name

    val running = result(accessStreamProperty.get(streamJar.id))
    running.state.get shouldBe ContainerState.RUNNING.name
    running.error.isEmpty shouldBe true

    // create the same streamApp cluster will get the previous stream cluster
    val prevRes = result(accessStreamAction.start(streamJar.id))
    prevRes.id shouldBe streamJar.id
    prevRes.name shouldBe streamAppName
    prevRes.state.get shouldBe ContainerState.RUNNING.name
    prevRes.error.isDefined shouldBe false

    // running streamApp cannot update state
    an[RuntimeException] should be thrownBy result(accessStreamProperty.update(streamJar.id, req.copy(instances = 10)))

    // running streamApp cannot delete
    an[RuntimeException] should be thrownBy result(accessStreamProperty.delete(streamJar.id))

    val res2 = result(accessStreamAction.stop(streamJar.id))
    res2.state shouldBe None
    res2.error shouldBe None

    // get property will get the latest state (streamApp not exist)
    val latest = result(accessStreamProperty.get(streamJar.id))
    latest.state shouldBe None
    latest.error.isDefined shouldBe false

    // after stop, streamApp can be deleted
    result(accessStreamProperty.delete(streamJar.id))

    // after delete, streamApp should not exist
    an[IllegalArgumentException] should be thrownBy result(accessStreamProperty.get(streamJar.id))

    file.deleteOnExit()
  }

  @Test
  def testStreamAppListPageFailCases(): Unit = {
    val newNameJar = StreamListRequest("new-name.jar")
    an[IllegalArgumentException] should be thrownBy result(accessStreamList.update("fake_id", newNameJar))
    an[NullPointerException] should be thrownBy result(accessStreamList.update("id", null))
    an[IllegalArgumentException] should be thrownBy
      result(accessStreamList.update("id", newNameJar.copy("")))
    an[IllegalArgumentException] should be thrownBy
      result(accessStreamList.update("id", newNameJar.copy(null)))
  }

  @Test
  def testStreamAppActionPageFailCases(): Unit = {
    val file = File.createTempFile("empty_", ".jar")
    val streamAppName = CommonUtils.assertOnlyNumberAndChar(CommonUtils.randomString(5))

    // upload jar
    val streamJar = result(accessStreamList.upload(Seq(file.getPath), None)).head

    // update properties
    val req = StreamPropertyRequest(streamAppName, Seq("topic1_id"), Seq("topic2_id"), 1)

    result(accessStreamProperty.update(streamJar.id, req.copy(from = Seq.empty)))
    an[IllegalArgumentException] should be thrownBy result(accessStreamAction.start(streamJar.id))

    result(accessStreamProperty.update(streamJar.id, req.copy(to = Seq.empty)))
    an[IllegalArgumentException] should be thrownBy result(accessStreamAction.start(streamJar.id))

    an[NullPointerException] should be thrownBy result(accessStreamProperty.update(streamJar.id, req.copy(from = null)))

    an[NullPointerException] should be thrownBy result(accessStreamProperty.update(streamJar.id, req.copy(to = null)))

    result(accessStreamProperty.update(streamJar.id, req.copy(instances = 0)))
    an[IllegalArgumentException] should be thrownBy result(accessStreamAction.start(streamJar.id))

    result(accessStreamProperty.update(streamJar.id, req.copy(instances = -99)))
    an[IllegalArgumentException] should be thrownBy result(accessStreamAction.start(streamJar.id))

    file.deleteOnExit()
  }

  @Test
  def duplicateDeleteStream(): Unit =
    (0 to 10).foreach(_ => result(accessStreamList.delete(CommonUtils.randomString(5))))

  @Test
  def duplicateDeleteStreamProperty(): Unit =
    (0 to 10).foreach(_ => result(accessStreamProperty.delete(CommonUtils.randomString(5))))
  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
