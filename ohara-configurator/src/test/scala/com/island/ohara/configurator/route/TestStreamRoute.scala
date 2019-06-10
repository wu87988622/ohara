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

import java.io.{File, RandomAccessFile}
import java.util.concurrent.TimeUnit

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
  def testStreamAppListPageWithoutCluster(): Unit = {
    val filePaths = Seq.fill(fileSize) {
      val file = File.createTempFile("empty_", ".jar")
      file.getPath
    }
    // upload files (without assign wkName, will upload to pre-defined worker cluster)
    result(accessStreamList.upload(filePaths, None)).foreach(jar => {
      jar.name.startsWith("empty_") shouldBe true
      CommonUtils.requireNonEmpty(jar.workerClusterName)
    })

    // without assign worker name, will get all files
    val res = result(accessStreamList.list(None))
    res.size shouldBe fileSize

    // get files with not exists cluster
    result(accessStreamList.list(Some("non_exist_worker_cluster"))).size shouldBe 0

    // delete first jar file
    val deleteStreamJar = res.head
    result(accessStreamList.delete(deleteStreamJar.id))
    result(accessStreamList.list(None)).size shouldBe fileSize - 1
    // Second time delete do nothing
    result(accessStreamList.delete(deleteStreamJar.id))

    // update last jar name
    val originJar = res.last
    val newNameJar = StreamListRequest("new-name.jar")
    val updated = result(accessStreamList.update(originJar.id, newNameJar))
    updated.name shouldBe "new-name.jar"

    filePaths.foreach(new File(_).deleteOnExit())
  }

  @Test
  def testStreamAppListPageCluster(): Unit = {
    val filePaths = Seq.fill(fileSize) {
      val file = File.createTempFile("empty_", ".jar")
      file.getPath
    }
    val wkName = result(configurator.clusterCollie.workerCollie().clusters).keys.head.name

    // upload with specific worker cluster
    result(accessStreamList.upload(filePaths, Some(wkName))).foreach(jar => {
      jar.name.startsWith("empty_") shouldBe true
      CommonUtils.requireNonEmpty(jar.workerClusterName)
    })
    // get jar list with specific worker cluster
    val jarInfos = result(accessStreamList.list(Some(wkName)))
    jarInfos.size shouldBe fileSize
    // without parameter, same result as previous
    result(accessStreamList.list(None)).size shouldBe fileSize

    result(accessStreamList.delete(jarInfos.head.id))
    // Second time delete do nothing
    result(accessStreamList.delete(jarInfos.head.id))
    result(accessStreamList.list(Some(wkName))).size shouldBe fileSize - 1

    filePaths.foreach(new File(_).deleteOnExit())
  }

  @Test
  def testStreamAppListPageFileLimit(): Unit = {
    val tmpFolder = CommonUtils.createTempFolder("fat")
    val outputFile = new File(tmpFolder, "fat.jar")
    val file = new RandomAccessFile(outputFile.getPath, "rw")
    // set out-of-bound size for file
    file.setLength(RouteUtils.DEFAULT_JAR_SIZE_BYTES + 1)
    file.close()

    // upload file should throw exception that the file size in out of limit (50MB)
    val expectedMessage = "exceeded content length limit (52428800 bytes)"
    val thrown = the[IllegalArgumentException] thrownBy result(accessStreamList.upload(Seq(outputFile.getPath), None))
    thrown.getMessage should include(expectedMessage)

    outputFile.deleteOnExit()
  }

  @Test
  def testStreamAppListPageCanUploadSameNameFiles(): Unit = {
    val file = File.createTempFile("empty_", ".jar")
    val wkName = result(configurator.clusterCollie.workerCollie().clusters).keys.head.name

    // upload file
    val res1 = result(accessStreamList.upload(Seq(file.getPath), Some(wkName)))
    // upload same file to same worker cluster is ok
    TimeUnit.SECONDS.sleep(5)
    file.setLastModified(System.currentTimeMillis())
    val res2 = result(accessStreamList.upload(Seq(file.getPath), Some(wkName)))

    // list jars should only show last modified jar
    val jars = result(accessStreamList.list(Some(wkName)))
    jars.size shouldBe 1
    jars.head.id should not be res1.head.id
    jars.head.id shouldBe res2.head.id

    file.deleteOnExit()
  }

  @Test
  def testStreamAppPropertyPage(): Unit = {
    val file = File.createTempFile("empty_", ".jar")

    val jarData = result(accessStreamList.upload(Seq(file.getPath), None))
    val jarId = jarData.head.id

    // create property
    val props = result(accessStreamProperty.add(StreamPropertyRequest(jarId, None, None, None, None)))

    // create property with some user defined properties
    val userProps = result(
      accessStreamProperty.add(StreamPropertyRequest(jarId, Some("bar foo"), None, Some(Seq("to")), Some(99))))
    userProps.name shouldBe "bar foo"
    userProps.to shouldBe Seq("to")
    userProps.instances shouldBe 99

    // get new streamApp property
    val res1 = result(accessStreamProperty.get(props.id))
    // check initial values
    res1.id shouldBe props.id
    res1.name shouldBe "Untitled stream app"
    res1.from.size shouldBe 0
    res1.to.size shouldBe 0
    res1.instances shouldBe 1

    // we create two properties, the list size should be 2
    result(accessStreamProperty.list).size shouldBe 2

    // update properties
    val appId = CommonUtils.randomString(5)
    val req = StreamPropertyRequest(jarId, Some(appId), Some(Seq("from")), Some(Seq("to")), Some(1))
    val res2 = result(accessStreamProperty.update(props.id, req))
    res2.name shouldBe appId
    res2.from.size shouldBe 1
    res2.to.size shouldBe 1
    res2.instances shouldBe 1

    // update partial properties
    val req1 = StreamPropertyRequest(jarId, None, None, Some(Seq("to1", "to2")), Some(10))
    val res3 = result(accessStreamProperty.update(props.id, req1))
    res3.name shouldBe appId
    res3.from.size shouldBe 1
    res3.to.size shouldBe 2
    res3.to shouldBe Seq("to1", "to2")
    res3.instances shouldBe 10

    // delete properties
    result(accessStreamProperty.delete(props.id))

    // after delete, the streamApp should not exist
    an[IllegalArgumentException] should be thrownBy result(accessStreamProperty.get(props.id))

    // delete property should not delete actual jar
    result(accessStreamList.list(None)).size shouldBe 1

    file.deleteOnExit()
  }

  @Test
  def testStreamAppAction(): Unit = {
    val file = File.createTempFile("empty_", ".jar")
    val instances = 3
    val streamAppName = CommonUtils.assertOnlyNumberAndChar(CommonUtils.randomString(5))

    // upload jar
    val streamJar = result(accessStreamList.upload(Seq(file.getPath), None)).head

    // create property
    val props = result(accessStreamProperty.add(StreamPropertyRequest(streamJar.id, None, None, None, None)))

    // update properties
    val req = StreamPropertyRequest(props.id,
                                    Some(streamAppName),
                                    Some(Seq("fromTopic_id")),
                                    Some(Seq("toTopic_id")),
                                    Some(instances))
    result(accessStreamProperty.update(props.id, req))

    val res1 = result(accessStreamAction.start(props.id))
    res1.id shouldBe props.id
    res1.name shouldBe streamAppName
    res1.workerClusterName should not be None
    res1.from shouldBe Seq("fromTopic_id")
    res1.to shouldBe Seq("toTopic_id")
    res1.jarInfo.name shouldBe streamJar.name
    res1.instances shouldBe instances
    res1.state.get shouldBe ContainerState.RUNNING.name

    val running = result(accessStreamProperty.get(props.id))
    running.state.get shouldBe ContainerState.RUNNING.name
    running.error.isEmpty shouldBe true

    // get the stream clusters information by clusterCache
    val cluster = result(configurator.clusterCollie.streamCollie().clusters)
    cluster.size shouldBe 1
    // jmx port should be positive
    cluster.head._1.jmxPort should not be 0

    // create the same streamApp cluster will get the previous stream cluster
    val prevRes = result(accessStreamAction.start(props.id))
    prevRes.id shouldBe props.id
    prevRes.name shouldBe streamAppName
    prevRes.state.get shouldBe ContainerState.RUNNING.name
    prevRes.error.isDefined shouldBe false

    // running streamApp cannot update state
    an[RuntimeException] should be thrownBy result(
      accessStreamProperty.update(props.id, req.copy(instances = Some(10))))

    // running streamApp cannot delete
    an[RuntimeException] should be thrownBy result(accessStreamProperty.delete(props.id))

    val res2 = result(accessStreamAction.stop(props.id))
    res2.state shouldBe None
    res2.error shouldBe None

    // get the stream clusters information again, should be zero
    result(configurator.clusterCollie.streamCollie().clusters).size shouldBe 0

    // stop the same streamApp cluster will only return the previous object
    val res3 = result(accessStreamAction.stop(props.id))
    res3.state shouldBe None
    res3.error shouldBe None

    // get property will get the latest state (streamApp not exist)
    val latest = result(accessStreamProperty.get(props.id))
    latest.state shouldBe None
    latest.error.isDefined shouldBe false

    // after stop, streamApp can be deleted
    result(accessStreamProperty.delete(props.id))

    // after delete, streamApp should not exist
    an[IllegalArgumentException] should be thrownBy result(accessStreamProperty.get(props.id))

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
  def testStreamAppPropertyPageFailCases(): Unit = {
    //operations on non-exists property should be fail
    val req =
      StreamPropertyRequest("jar_id", Some("app-id"), Some(Seq("fromTopic_id")), Some(Seq("toTopic_id")), Some(0))
    an[IllegalArgumentException] should be thrownBy result(accessStreamProperty.update("non_exist_id", req))
    an[IllegalArgumentException] should be thrownBy result(accessStreamProperty.get("non_exist_id"))

    // delete non-exists object should do nothing
    result(accessStreamProperty.delete("non_exist_id"))
  }

  @Test
  def testStreamAppActionPageFailCases(): Unit = {
    val file = File.createTempFile("empty_", ".jar")
    val streamAppName = CommonUtils.assertOnlyNumberAndChar(CommonUtils.randomString(5))

    // upload jar
    val streamJar = result(accessStreamList.upload(Seq(file.getPath), None)).head

    val req =
      StreamPropertyRequest(streamJar.id, Some(streamAppName), Some(Seq("topic1_id")), Some(Seq("topic2_id")), Some(1))

    // create property
    val props = result(accessStreamProperty.add(req))

    // start action will check all the required parameters
    result(accessStreamProperty.update(props.id, req.copy(from = Some(Seq.empty))))
    an[IllegalArgumentException] should be thrownBy result(accessStreamAction.start(props.id))

    result(accessStreamProperty.update(props.id, req.copy(to = Some(Seq.empty))))
    an[IllegalArgumentException] should be thrownBy result(accessStreamAction.start(props.id))

    result(accessStreamProperty.update(props.id, req.copy(instances = Some(0))))
    an[IllegalArgumentException] should be thrownBy result(accessStreamAction.start(props.id))

    result(accessStreamProperty.update(props.id, req.copy(instances = Some(-99))))
    an[IllegalArgumentException] should be thrownBy result(accessStreamAction.start(props.id))

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
