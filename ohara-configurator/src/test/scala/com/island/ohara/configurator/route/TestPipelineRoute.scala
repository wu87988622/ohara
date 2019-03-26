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

import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorCreationRequest
import com.island.ohara.client.configurator.v0.HadoopApi.HdfsInfoRequest
import com.island.ohara.client.configurator.v0.NodeApi.NodeCreationRequest
import com.island.ohara.client.configurator.v0.PipelineApi.{Flow, ObjectState, Pipeline, PipelineCreationRequest}
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.configurator.v0._
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.{Configurator, DumbSink}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestPipelineRoute extends SmallTest with Matchers {
  private[this] val configurator = Configurator.builder().fake(1, 1).build()

  private[this] def result[T](f: Future[T]): T = Await.result(f, 30 seconds)

  private[this] val pipelineApi = PipelineApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] val connectorApi = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)
  @Test
  def testMultiWorkerCluster(): Unit = {

    val pipeline0 = result(
      pipelineApi.add(
        PipelineCreationRequest(
          name = CommonUtils.randomString(10),
          workerClusterName = None,
          rules = Map.empty
        ))
    )
    pipeline0.workerClusterName shouldBe result(
      configurator.clusterCollie.workerCollie().cluster(pipeline0.workerClusterName))._1.name

    // add node
    result(
      NodeApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          NodeCreationRequest(
            name = Some(methodName()),
            port = 22,
            user = CommonUtils.randomString(10),
            password = CommonUtils.randomString(10)
          ))
    )
    val wkCluster = result(
      WorkerApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(WorkerApi.creationRequest(CommonUtils.randomString(10), Seq(methodName())))
    )

    val pipeline1 = result(
      pipelineApi.add(
        PipelineCreationRequest(
          name = CommonUtils.randomString(10),
          workerClusterName = Some(wkCluster.name),
          rules = Map.empty
        ))
    )

    pipeline1.workerClusterName shouldBe wkCluster.name

    val pipelines = result(pipelineApi.list)
    pipelines.size shouldBe 2
    pipelines.find(_.id == pipeline0.id).get.workerClusterName shouldBe pipeline0.workerClusterName
    pipelines.find(_.id == pipeline1.id).get.workerClusterName shouldBe pipeline1.workerClusterName
  }

  @Test
  def testNormalCase(): Unit = {
    val connector = result(
      connectorApi.add(
        ConnectorCreationRequest(
          workerClusterName = None,
          className = Some(classOf[DumbSink].getName),
          topicNames = Seq.empty,
          numberOfTasks = Some(1),
          columns = Seq.empty,
          settings = Map.empty
        ))
    )

    val topic = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None))
    )

    val pipeline = result(
      pipelineApi.add(
        PipelineCreationRequest(
          name = CommonUtils.randomString(10),
          workerClusterName = None,
          rules = Map(connector.id -> Seq(topic.id))
        ))
    )

    pipeline.rules.size shouldBe 1
    pipeline.rules(connector.id) shouldBe Seq(topic.id)
    pipeline.objects.size should not be 0

    // remove connector
    result(connectorApi.delete(connector.id))

    val pipeline2 = result(pipelineApi.get(pipeline.id))

    pipeline2.rules.size shouldBe 0

    val pipelines = result(pipelineApi.list)

    pipelines.size shouldBe 1
    pipelines.head.rules.size shouldBe 0
  }

  @Test
  def testPipeline(): Unit = {
    def compareRequestAndResponse(request: PipelineCreationRequest, response: Pipeline): Pipeline = {
      request.name shouldBe response.name
      request.rules shouldBe response.rules
      response
    }

    def compare2Response(lhs: Pipeline, rhs: Pipeline): Unit = {
      lhs.id shouldBe rhs.id
      lhs.name shouldBe rhs.name
      lhs.rules shouldBe rhs.rules
      lhs.objects shouldBe rhs.objects
    }

    // test add
    val topicAccess = TopicApi.access().hostname(configurator.hostname).port(configurator.port)
    val uuid_0 = result(
      topicAccess.add(
        TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                             brokerClusterName = None,
                             numberOfPartitions = None,
                             numberOfReplications = None))).id
    val uuid_1 = result(
      topicAccess.add(
        TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                             brokerClusterName = None,
                             numberOfPartitions = None,
                             numberOfReplications = None))).id
    val uuid_2 = result(
      topicAccess.add(
        TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                             brokerClusterName = None,
                             numberOfPartitions = None,
                             numberOfReplications = None))).id

    result(pipelineApi.list).size shouldBe 0

    val request =
      PipelineCreationRequest(name = methodName, workerClusterName = None, rules = Map(uuid_0 -> Seq(uuid_1)))
    val response = compareRequestAndResponse(request, result(pipelineApi.add(request)))

    // test get
    compare2Response(response, result(pipelineApi.get(response.id)))

    // test update
    val anotherRequest =
      PipelineCreationRequest(name = methodName, workerClusterName = None, rules = Map(uuid_0 -> Seq(uuid_2)))
    val newResponse =
      compareRequestAndResponse(anotherRequest, result(pipelineApi.update(response.id, anotherRequest)))

    // topics should have no state
    newResponse.objects.foreach(_.state shouldBe None)

    // test get
    compare2Response(newResponse, result(pipelineApi.get(newResponse.id)))

    // test delete
    result(pipelineApi.list).size shouldBe 1
    result(pipelineApi.delete(response.id)) shouldBe newResponse
    result(pipelineApi.list).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(pipelineApi.get("asdasdsad"))
    an[IllegalArgumentException] should be thrownBy result(pipelineApi.update("asdasdsad", anotherRequest))
  }

  @Test
  def testBindInvalidObjects2Pipeline(): Unit = {
    val topicAccess = TopicApi.access().hostname(configurator.hostname).port(configurator.port)
    val hdfsAccess = HadoopApi.access().hostname(configurator.hostname).port(configurator.port)
    val uuid_0 = result(
      topicAccess.add(
        TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                             brokerClusterName = None,
                             numberOfPartitions = None,
                             numberOfReplications = None))).id
    val uuid_1 = result(hdfsAccess.add(HdfsInfoRequest(methodName, "file:///"))).id
    val uuid_2 = result(hdfsAccess.add(HdfsInfoRequest(methodName, "file:///"))).id
    val uuid_3 = result(
      topicAccess.add(
        TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                             brokerClusterName = None,
                             numberOfPartitions = None,
                             numberOfReplications = None))).id
    result(topicAccess.list).size shouldBe 2
    result(hdfsAccess.list).size shouldBe 2

    // uuid_0 -> uuid_0: self-bound
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.add(
        PipelineCreationRequest(name = methodName, workerClusterName = None, rules = Map(uuid_0 -> Seq(uuid_0)))))
    // uuid_1 can't be applied to pipeline
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.add(
        PipelineCreationRequest(name = methodName, workerClusterName = None, rules = Map(uuid_0 -> Seq(uuid_1)))))
    // uuid_2 can't be applied to pipeline
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.add(
        PipelineCreationRequest(name = methodName, workerClusterName = None, rules = Map(uuid_0 -> Seq(uuid_2)))))

    val res = result(
      pipelineApi.add(
        PipelineCreationRequest(name = methodName, workerClusterName = None, rules = Map(uuid_0 -> Seq(uuid_3)))))
    // uuid_0 -> uuid_0: self-bound
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.update(
        res.id,
        PipelineCreationRequest(name = methodName, workerClusterName = None, rules = Map(uuid_0 -> Seq(uuid_0)))))
    // uuid_1 can't be applied to pipeline
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.update(
        res.id,
        PipelineCreationRequest(name = methodName, workerClusterName = None, rules = Map(uuid_0 -> Seq(uuid_1)))))
    // uuid_2 can't be applied to pipeline
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.update(
        res.id,
        PipelineCreationRequest(name = methodName, workerClusterName = None, rules = Map(uuid_0 -> Seq(uuid_2)))))

    // good case
    result(
      pipelineApi.update(res.id,
                         PipelineCreationRequest(name = methodName,
                                                 workerClusterName = None,
                                                 rules = Map(uuid_0 -> Seq(uuid_3))))).name shouldBe methodName
  }

  @Test
  def unknownValueShouldBeFiltered(): Unit = {
    val topic = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None))
    )

    val pipeline = result(
      pipelineApi.add(
        PipelineCreationRequest(
          name = CommonUtils.randomString(10),
          workerClusterName = None,
          flows = Seq(
            Flow(
              from = topic.id,
              to = Seq.empty
            )
          )
        ))
    )

    pipeline.rules.size shouldBe 1
    pipeline.rules(topic.id).size shouldBe 0
  }

  @Test
  def removeConnectorFromDeletedCluster(): Unit = {
    val topic = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None))
    )

    val pipeline = result(
      pipelineApi.add(
        PipelineCreationRequest(
          name = CommonUtils.randomString(10),
          workerClusterName = None,
          flows = Seq(
            Flow(
              from = topic.id,
              to = Seq.empty
            )
          )
        )))

    val wk = result(configurator.clusterCollie.workerCollie().remove(pipeline.workerClusterName))
    wk.name shouldBe pipeline.workerClusterName

    result(pipelineApi.delete(pipeline.id))

    result(pipelineApi.list).exists(_.id == pipeline.id) shouldBe false
  }

  @Test
  def listPipelineWithoutWorkerCluster(): Unit = {
    val topic = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None))
    )

    val pipeline = result(
      pipelineApi.add(
        PipelineCreationRequest(
          name = CommonUtils.randomString(10),
          workerClusterName = None,
          flows = Seq(
            Flow(
              from = topic.id,
              to = Seq.empty
            )
          )
        )))
    pipeline.rules.isEmpty shouldBe false
    pipeline.rules(topic.id).size shouldBe 0

    val wk = result(configurator.clusterCollie.workerCollie().remove(pipeline.workerClusterName))
    wk.name shouldBe pipeline.workerClusterName

    val anotherPipeline = result(pipelineApi.list).find(_.id == pipeline.id).get

    anotherPipeline.id shouldBe pipeline.id
    anotherPipeline.rules shouldBe pipeline.rules
    // worker cluster is gone so it fails to fetch objects status
    anotherPipeline.objects.size shouldBe 0
  }

  @Test
  def listPipelineWithoutWorkerCluster2(): Unit = {
    val topic0 = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None))
    )

    val topic1 = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None))
    )

    val pipeline = result(
      pipelineApi.add(
        PipelineCreationRequest(
          name = CommonUtils.randomString(10),
          workerClusterName = None,
          flows = Seq(
            Flow(
              from = topic0.id,
              to = Seq(topic1.id)
            )
          )
        )))
    pipeline.rules.isEmpty shouldBe false
    pipeline.rules(topic0.id).size shouldBe 1
    pipeline.rules(topic0.id).contains(topic1.id) shouldBe true

    val wk = result(configurator.clusterCollie.workerCollie().remove(pipeline.workerClusterName))
    wk.name shouldBe pipeline.workerClusterName

    val anotherPipeline = result(pipelineApi.list).find(_.id == pipeline.id).get

    anotherPipeline.id shouldBe pipeline.id
    anotherPipeline.rules shouldBe pipeline.rules
    // worker cluster is gone so it fails to fetch objects status
    anotherPipeline.objects.size shouldBe 0
  }

  @Test
  def addPipelineWithoutCluster(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.add(
        PipelineCreationRequest(name = CommonUtils.randomString(10),
                                workerClusterName = Some(CommonUtils.randomString(10)),
                                rules = Map.empty)
      ))
  }

  @Test
  def addMultiPipelines(): Unit = {
    val topic0 = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None))
    )

    val topic1 = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None))
    )

    val pipeline0 = result(
      pipelineApi.add(
        PipelineCreationRequest(
          name = CommonUtils.randomString(10),
          workerClusterName = None,
          flows = Seq(
            Flow(
              from = topic0.id,
              to = Seq(topic1.id)
            )
          )
        )))

    val pipeline1 = result(
      pipelineApi.add(
        PipelineCreationRequest(
          name = CommonUtils.randomString(10),
          workerClusterName = None,
          flows = Seq(
            Flow(
              from = topic0.id,
              to = Seq(topic1.id)
            )
          )
        )))

    val pipelines = result(pipelineApi.list)
    pipelines.size shouldBe 2
    pipelines.exists(_.id == pipeline0.id) shouldBe true
    pipelines.exists(_.id == pipeline1.id) shouldBe true
  }

  @Test
  def listConnectorWhichIsNotRunning(): Unit = {
    val topic = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None))
    )

    val connector = result(
      connectorApi.add(
        ConnectorCreationRequest(
          workerClusterName = None,
          className = Some(classOf[DumbSink].getName),
          topicNames = Seq.empty,
          numberOfTasks = Some(1),
          columns = Seq.empty,
          settings = Map.empty
        ))
    )

    val pipeline = result(
      pipelineApi.add(
        PipelineCreationRequest(
          name = CommonUtils.randomString(10),
          workerClusterName = None,
          rules = Map(topic.id -> Seq(connector.id))
        )))
    pipeline.objects.size shouldBe 2
    pipeline.objects.foreach { obj =>
      obj.error shouldBe None
      obj.state shouldBe None
    }
  }

  @Test
  def useWrongConnector(): Unit = {
    val topic = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None))
    )

    val connector = result(
      connectorApi.add(
        ConnectorCreationRequest(
          workerClusterName = None,
          className = Some(CommonUtils.randomString()),
          topicNames = Seq.empty,
          numberOfTasks = Some(1),
          columns = Seq.empty,
          settings = Map.empty
        ))
    )

    val pipeline = result(
      pipelineApi.add(
        PipelineCreationRequest(
          name = CommonUtils.randomString(10),
          workerClusterName = None,
          rules = Map(topic.id -> Seq(connector.id))
        )))
    pipeline.objects.size shouldBe 2
    pipeline.objects.filter(_.id == connector.id).foreach { obj =>
      obj.error.isEmpty shouldBe false
      obj.state shouldBe None
    }
  }

  @Test
  def testPipelineStateAfterStartingSource(): Unit = {
    val topic = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None))
    )
    val sourceRequest = ConnectorCreationRequest(
      workerClusterName = None,
      className = Some(classOf[DumbSink].getName),
      columns = Seq.empty,
      topicNames = Seq(topic.id),
      settings = Map.empty,
      numberOfTasks = Some(1)
    )

    val source = result(connectorApi.add(sourceRequest))
    val pipeline = result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          PipelineCreationRequest(
            name = "abc",
            workerClusterName = None,
            flows = Seq(
              Flow(
                from = source.id,
                to = Seq.empty
              )
            )
          )
        )
    )
    pipeline.objects.foreach(obj => obj.state shouldBe None)

    // start source and pipeline should "see" what happen in source
    // we don't want to compare state since the state may be changed
    result(connectorApi.start(source.id)).copy(state = None) shouldBe source.copy(state = None)
    val pipeline2 = result(
      PipelineApi.access().hostname(configurator.hostname).port(configurator.port).get(pipeline.id)
    )
    pipeline2.objects.foreach(
      obj => obj.state.get shouldBe ObjectState.RUNNING
    )
  }

  @Test
  def testPipelineAllowObject(): Unit = {
    val pipeline = result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          PipelineCreationRequest(
            name = "testPipelineAllowData",
            workerClusterName = None,
            rules = Map.empty
          )
        )
    )

    val topic = result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None))
    )

    val sourceRequest = ConnectorCreationRequest(
      workerClusterName = None,
      className = Some("jdbc"),
      columns = Seq.empty,
      topicNames = Seq(topic.id),
      settings = Map.empty,
      numberOfTasks = Some(1)
    )
    val source = result(connectorApi.add(sourceRequest))

    val filePath = File.createTempFile("empty_", ".jar").getPath
    val streamapp = result(
      StreamApi
        .accessOfList()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .upload(pipeline.id, Seq(filePath))
    )

    result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .update(
          pipeline.id,
          PipelineCreationRequest(
            name = "abc",
            workerClusterName = None,
            rules = Map(source.id -> Seq(topic.id))
          )
        )
    ).objects.size shouldBe 2
    result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .update(
          pipeline.id,
          PipelineCreationRequest(
            name = "abc",
            workerClusterName = None,
            rules = Map(topic.id -> Seq(streamapp.head.id))
          )
        )
    ).objects.size shouldBe 2
  }

  @Test
  def testToUnknownObject(): Unit = {
    val sourceRequest = ConnectorCreationRequest(
      workerClusterName = None,
      className = Some("jdbc"),
      columns = Seq.empty,
      topicNames = Seq.empty,
      settings = Map.empty,
      numberOfTasks = Some(1)
    )

    val source = result(connectorApi.add(sourceRequest))

    an[IllegalArgumentException] should be thrownBy result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          PipelineCreationRequest(
            name = "abc",
            workerClusterName = None,
            flows = Seq(
              Flow(
                from = source.id,
                to = Seq(CommonUtils.randomString())
              )
            )
          )
        )
    )

    val source2 = result(connectorApi.add(sourceRequest))
    an[IllegalArgumentException] should be thrownBy result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          PipelineCreationRequest(
            name = "abc",
            workerClusterName = None,
            flows = Seq(
              Flow(
                from = source.id,
                to = Seq(source2.id, CommonUtils.randomString())
              )
            )
          )
        )
    )
  }

  @Test
  def testFromUnknownObject(): Unit = {
    val sourceRequest = ConnectorCreationRequest(
      workerClusterName = None,
      className = Some("jdbc"),
      columns = Seq.empty,
      topicNames = Seq.empty,
      settings = Map.empty,
      numberOfTasks = Some(1)
    )

    val source = result(connectorApi.add(sourceRequest))

    an[IllegalArgumentException] should be thrownBy result(
      PipelineApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          PipelineCreationRequest(
            name = "abc",
            workerClusterName = None,
            flows = Seq(
              Flow(
                from = CommonUtils.randomString(),
                to = Seq(source.id)
              )
            )
          )
        )
    )
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
