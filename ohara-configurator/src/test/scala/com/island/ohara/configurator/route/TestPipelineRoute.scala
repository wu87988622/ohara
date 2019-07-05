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

import com.island.ohara.client.configurator.v0.JarApi.JarKey
import com.island.ohara.client.configurator.v0.PipelineApi.Flow
import com.island.ohara.client.configurator.v0._
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.{Configurator, DumbSink}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global

// there are too many test cases in this file so we promote  it from small test to medium test
class TestPipelineRoute extends MediumTest with Matchers {
  private[this] val configurator = Configurator.builder().fake(1, 1).build()

  private[this] val workerApi = WorkerApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] val pipelineApi = PipelineApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] val connectorApi = ConnectorApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] val topicApi = TopicApi.access.hostname(configurator.hostname).port(configurator.port)

  @Test
  def testMultiWorkerCluster(): Unit = {

    val pipeline0 = result(
      pipelineApi.request.name(CommonUtils.randomString(10)).create()
    )
    pipeline0.workerClusterName shouldBe result(
      configurator.clusterCollie.workerCollie().cluster(pipeline0.workerClusterName))._1.name

    // add node
    val addNodeName: String = methodName().toLowerCase
    result(
      NodeApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(addNodeName)
        .port(22)
        .user(CommonUtils.randomString())
        .password(CommonUtils.randomString())
        .create()
    )
    val wkCluster = result(
      WorkerApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name(CommonUtils.randomString(10))
        .nodeName(addNodeName)
        .create()
    )

    val pipeline1 = result(
      pipelineApi.request.name(CommonUtils.randomString(10)).workerClusterName(wkCluster.name).create()
    )

    pipeline1.workerClusterName shouldBe wkCluster.name

    val pipelines = result(pipelineApi.list())
    pipelines.size shouldBe 2
    pipelines.find(_.name == pipeline0.name).get.workerClusterName shouldBe pipeline0.workerClusterName
    pipelines.find(_.name == pipeline1.name).get.workerClusterName shouldBe pipeline1.workerClusterName
  }

  @Test
  def testNormalCase(): Unit = {
    val connector = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .numberOfTasks(1)
        .create())

    val topic = result(topicApi.request.name(CommonUtils.randomString(10)).create())

    val pipeline = result(
      pipelineApi.request.name(CommonUtils.randomString(10)).flow(Flow(connector.name, Set(topic.name))).create()
    )

    pipeline.flows.size shouldBe 1
    pipeline.flows.head.from shouldBe connector.name
    pipeline.flows.head.to shouldBe Set(topic.name)
    pipeline.objects.size should not be 0

    // remove connector
    result(connectorApi.delete(connector.name))

    val pipeline2 = result(pipelineApi.get(pipeline.name))

    pipeline2.flows.size shouldBe 0

    val pipelines = result(pipelineApi.list())

    pipelines.size shouldBe 1
    pipelines.head.flows.size shouldBe 0
  }

  @Test
  def testPipeline(): Unit = {
    // test add
    val topic0 = result(topicApi.request.name(CommonUtils.randomString(10)).create())
    val topic1 = result(topicApi.request.name(CommonUtils.randomString(10)).create())
    val topic2 = result(topicApi.request.name(CommonUtils.randomString(10)).create())

    result(pipelineApi.list()).size shouldBe 0

    val name = CommonUtils.randomString()
    val flow = Flow(from = topic0.name, to = Set(topic1.name, topic2.name))
    val pipeline = result(pipelineApi.request.name(name).flow(flow).create())

    result(pipelineApi.list()).size shouldBe 1
    pipeline.name shouldBe name
    pipeline.flows.size shouldBe 1
    pipeline.flows.head shouldBe flow

    // test get
    val pipeline2 = result(pipelineApi.get(pipeline.name))
    pipeline.name shouldBe pipeline2.name
    pipeline.workerClusterName shouldBe pipeline2.workerClusterName
    pipeline.flows shouldBe pipeline2.flows

    val flow3 = Flow(from = topic0.name, to = Set.empty)

    // test update
    val pipeline3 = result(pipelineApi.request.name(pipeline.name).flow(flow3).update())

    pipeline3.flows.size shouldBe 1
    pipeline3.flows.head shouldBe flow3

    // test delete
    result(pipelineApi.list()).size shouldBe 1
    result(pipelineApi.delete(pipeline3.name))
    result(pipelineApi.list()).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(pipelineApi.get(CommonUtils.randomString()))
  }

  @Test
  def testBindInvalidObjects2Pipeline(): Unit = {
    val hdfsAccess = HadoopApi.access.hostname(configurator.hostname).port(configurator.port)
    val topic0 = result(topicApi.request.name(CommonUtils.randomString(10)).create())
    val topic1 = result(topicApi.request.name(CommonUtils.randomString(10)).create())
    val hdfs = result(hdfsAccess.request.name(CommonUtils.randomString()).uri("file:///").create())
    result(topicApi.list()).size shouldBe 2
    result(hdfsAccess.list()).size shouldBe 1

    // hdfs0 is hdfs info so it can't be applied to pipeline
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.request.name(CommonUtils.randomString()).flow(topic0.name, hdfs.name).create())

    val pipeline = result(pipelineApi.request.name(CommonUtils.randomString()).flow(topic0.name, topic1.name).create())

    result(pipelineApi.list()).size shouldBe 1

    // hdfs0 is hdfs info so it can't be applied to pipeline
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.request.name(pipeline.name).flow(topic0.name, hdfs.name).update())

    // good case
    result(pipelineApi.request.name(pipeline.name).flow(topic0.name, Set.empty[String]).update())
    result(pipelineApi.list()).size shouldBe 1
  }

  @Test
  def removeConnectorFromDeletedCluster(): Unit = {
    val topic = result(topicApi.request.name(CommonUtils.randomString(10)).create())

    val pipeline = result(
      pipelineApi.request.name(CommonUtils.randomString()).flow(topic.name, Set.empty[String]).create())

    pipeline.workerClusterName shouldBe result(configurator.clusterCollie.workerCollie().clusters).head._1.name

    result(configurator.clusterCollie.workerCollie().remove(pipeline.workerClusterName))

    result(pipelineApi.delete(pipeline.name))

    result(pipelineApi.list()).exists(_.name == pipeline.name) shouldBe false
  }

  @Test
  def listPipelineWithoutWorkerCluster(): Unit = {
    val topic = result(topicApi.request.name(CommonUtils.randomString(10)).create())

    val pipeline = result(
      pipelineApi.request.name(CommonUtils.randomString()).flow(topic.name, Set.empty[String]).create())

    pipeline.flows.size shouldBe 1
    pipeline.flows.head.from shouldBe topic.name
    pipeline.flows.head.to shouldBe Set.empty[String]

    result(configurator.clusterCollie.workerCollie().remove(pipeline.workerClusterName))

    val anotherPipeline = result(pipelineApi.list()).find(_.name == pipeline.name).get

    anotherPipeline.name shouldBe pipeline.name
    anotherPipeline.flows shouldBe pipeline.flows
    // worker cluster is gone so it fails to fetch objects status
    anotherPipeline.objects.size shouldBe 0
  }

  @Test
  def listPipelineWithoutWorkerCluster2(): Unit = {
    val topic0 = result(topicApi.request.name(CommonUtils.randomString(10)).create())

    val topic1 = result(topicApi.request.name(CommonUtils.randomString(10)).create())

    val pipeline = result(pipelineApi.request.name(CommonUtils.randomString()).flow(topic0.name, topic1.name).create())

    pipeline.flows.size shouldBe 1
    pipeline.flows.head.from shouldBe topic0.name
    pipeline.flows.head.to shouldBe Set(topic1.name)

    result(configurator.clusterCollie.workerCollie().remove(pipeline.workerClusterName))

    val anotherPipeline = result(pipelineApi.list()).find(_.name == pipeline.name).get

    anotherPipeline.name shouldBe pipeline.name
    anotherPipeline.flows shouldBe pipeline.flows
    // worker cluster is gone so it fails to fetch objects status
    anotherPipeline.objects.size shouldBe 0
  }

  @Test
  def addPipelineWithUnknownCluster(): Unit =
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.request.name(CommonUtils.randomString()).workerClusterName(CommonUtils.randomString()).create()
    )

  @Test
  def addMultiPipelines(): Unit = {
    val topic0 = result(topicApi.request.name(CommonUtils.randomString(10)).create())

    val topic1 = result(topicApi.request.name(CommonUtils.randomString(10)).create())

    val pipeline0 = result(pipelineApi.request.name(CommonUtils.randomString()).flow(topic0.name, topic1.name).create())

    val pipeline1 = result(pipelineApi.request.name(CommonUtils.randomString()).flow(topic0.name, topic1.name).create())

    val pipelines = result(pipelineApi.list())
    pipelines.size shouldBe 2
    pipelines.exists(_.name == pipeline0.name) shouldBe true
    pipelines.exists(_.name == pipeline1.name) shouldBe true
  }

  @Test
  def listConnectorWhichIsNotRunning(): Unit = {
    val topic = result(topicApi.request.name(CommonUtils.randomString(10)).create())

    val connector = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .numberOfTasks(1)
        .create())

    val pipeline = result(
      pipelineApi.request.name(CommonUtils.randomString()).flow(topic.name, connector.name).create())

    pipeline.objects.size shouldBe 2
    pipeline.objects.foreach { obj =>
      obj.error shouldBe None
      obj.state shouldBe None
    }
  }

  @Test
  def useWrongConnector(): Unit = {
    val topic = result(topicApi.request.name(CommonUtils.randomString(10)).create())

    val connector = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(CommonUtils.randomString(10))
        .numberOfTasks(1)
        .create())

    val pipeline = result(
      pipelineApi.request.name(CommonUtils.randomString()).flow(topic.name, connector.name).create())

    pipeline.objects.size shouldBe 2
    pipeline.objects.filter(_.name == connector.name).foreach { obj =>
      obj.error.isEmpty shouldBe false
      obj.state shouldBe None
    }
  }

  @Test
  def testPipelineStateAfterStartingSource(): Unit = {
    val topic = result(topicApi.request.name(CommonUtils.randomString(10)).create())

    val source = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .numberOfTasks(1)
        .topicName(topic.name)
        .create())

    val pipeline = result(
      pipelineApi.request.name(CommonUtils.randomString()).flow(source.name, Set.empty[String]).create())

    pipeline.objects.foreach(obj => obj.state shouldBe None)

    // start source and pipeline should "see" what happen in source
    // we don't want to compare state since the state may be changed
    result(connectorApi.start(source.name)).copy(state = None) shouldBe source.copy(state = None)
    val pipeline2 = result(
      PipelineApi.access.hostname(configurator.hostname).port(configurator.port).get(pipeline.name)
    )
    pipeline2.objects.foreach(
      obj => obj.state.get shouldBe "RUNNING"
    )
  }

  @Test
  def testPipelineAllowObject(): Unit = {
    val topic = result(topicApi.request.name(CommonUtils.randomString(10)).create())
    val source = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .numberOfTasks(1)
        .create())

    val wkName = result(workerApi.list()).head.name
    val file = File.createTempFile("empty_", ".jar")
    val jar = result(
      JarApi.access.hostname(configurator.hostname).port(configurator.port).request.group(wkName).upload(file))
    val streamapp = result(
      StreamApi.accessOfProperty
        .hostname(configurator.hostname)
        .port(configurator.port)
        .request
        .name("appid")
        .jar(JarKey(jar.group, jar.name))
        .from(Set("from"))
        .to(Set("to"))
        .instances(1)
        .create())

    result(pipelineApi.request.name(CommonUtils.randomString()).flow(source.name, topic.name).create()).objects.size shouldBe 2

    result(pipelineApi.request.name(CommonUtils.randomString()).flow(source.name, streamapp.name).create()).objects.size shouldBe 2
  }

  @Test
  def testToUnknownObject(): Unit = {
    val source = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .numberOfTasks(1)
        .create())

    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.request.name(CommonUtils.randomString()).flow(source.name, CommonUtils.randomString()).create())

    val source2 = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .numberOfTasks(1)
        .create())
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.request
        .name(CommonUtils.randomString())
        .flow(source.name, Set(source2.name, CommonUtils.randomString()))
        .create())
  }

  @Test
  def testFromUnknownObject(): Unit = {
    val source = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .numberOfTasks(1)
        .create())

    an[IllegalArgumentException] should be thrownBy
      result(
        pipelineApi.request.name(CommonUtils.randomString()).flow(CommonUtils.randomString(), source.name).create())
  }

  @Test
  def duplicateDelete(): Unit =
    (0 to 10).foreach(_ => result(pipelineApi.delete(CommonUtils.randomString(5))))

  @Test
  def duplicateUpdate(): Unit = {
    val count = 10
    (0 until count).foreach(_ => result(pipelineApi.request.name(CommonUtils.randomString()).flows(Seq.empty).update()))
    result(pipelineApi.list()).size shouldBe count
  }

  @Test
  def updatingNonexistentNameCanNotIgnoreFlows(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.request.name(CommonUtils.randomString()).update())
    val name = CommonUtils.randomString()
    val flows: Seq[Flow] = Seq.empty
    val pipeline = result(pipelineApi.request.name(name).flows(flows).update())
    result(pipelineApi.list()).size shouldBe 1
    pipeline.name shouldBe name
    pipeline.flows shouldBe flows
  }

  @Test
  def updateOnlyFlow(): Unit = {
    val topic = result(topicApi.request.name(CommonUtils.randomString(10)).create())
    val pipeline = result(
      pipelineApi.request.name(CommonUtils.randomString()).flow(topic.name, Set.empty[String]).update())
    pipeline.flows.size shouldBe 1
    pipeline.flows.head.from shouldBe topic.name
    pipeline.flows.head.to.size shouldBe 0

    val pipeline2 = result(pipelineApi.request.name(pipeline.name).flows(Seq.empty).update())
    result(pipelineApi.list()).size shouldBe 1
    pipeline2.name shouldBe pipeline.name
    pipeline2.flows shouldBe Seq.empty
  }

  @Test
  def updateOnlyWorkerClusterName(): Unit = {
    val topic = result(topicApi.request.name(CommonUtils.randomString(10)).create())
    val pipeline = result(
      pipelineApi.request.name(CommonUtils.randomString()).flow(topic.name, Set.empty[String]).update())
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.request.name(pipeline.name).workerClusterName(CommonUtils.randomString()).update())
  }

  @Test
  def testRemovePipelineHavingRunningConnector(): Unit = {
    val topic = result(topicApi.request.name(CommonUtils.randomString(10)).create())

    val connector = result(
      connectorApi.request
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .topicName(topic.name)
        .numberOfTasks(1)
        .create())

    val pipeline = result(pipelineApi.request.name(methodName()).flow(topic.name, connector.name).create())

    // start the connector
    result(connectorApi.start(connector.name)).state should not be None

    // we can't delete a pipeline having a running connector

    an[IllegalArgumentException] should be thrownBy result(pipelineApi.delete(pipeline.name))

    // now we stop the connector
    result(connectorApi.stop(connector.name)).state shouldBe None

    // and then it is ok to delete pipeline
    result(pipelineApi.delete(pipeline.name))

    // let check the existence of topic
    result(topicApi.list()).size shouldBe 1

    // let check the existence of connector
    result(connectorApi.list()).size shouldBe 0
  }

  @Test
  def testDuplicateObjectName(): Unit = {
    val name = CommonUtils.randomString(10)
    val topic = result(topicApi.request.name(name).create())

    val connector = result(
      connectorApi.request
        .name(name)
        .className(classOf[DumbSink].getName)
        .topicName(topic.name)
        .numberOfTasks(1)
        .create())

    val pipeline = result(pipelineApi.request.name(methodName()).flow(topic.name, connector.name).create())
    pipeline.flows.size shouldBe 1
    pipeline.objects.size shouldBe 2
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
