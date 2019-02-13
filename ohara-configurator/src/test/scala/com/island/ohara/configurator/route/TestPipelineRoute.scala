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

import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorCreationRequest
import com.island.ohara.client.configurator.v0.HadoopApi.HdfsInfoRequest
import com.island.ohara.client.configurator.v0.NodeApi.NodeCreationRequest
import com.island.ohara.client.configurator.v0.PipelineApi.{Pipeline, PipelineCreationRequest}
import com.island.ohara.client.configurator.v0.WorkerApi.WorkerClusterCreationRequest
import com.island.ohara.client.configurator.v0._
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.Releasable
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestPipelineRoute extends SmallTest with Matchers {
  private[this] val configurator = Configurator.builder().fake(1, 1).build()

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  private[this] val pipelineApi = PipelineApi.access().hostname(configurator.hostname).port(configurator.port)

  @Test
  def testMultiWorkerCluster(): Unit = {

    val pipeline0 = result(
      pipelineApi.add(
        PipelineCreationRequest(
          name = methodName(),
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
            user = methodName(),
            password = methodName()
          ))
    )
    val wkCluster = result(
      WorkerApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          WorkerClusterCreationRequest(
            name = methodName(),
            imageName = None,
            brokerClusterName = None,
            clientPort = None,
            nodeNames = Seq(methodName()),
            jars = Seq.empty
          ))
    )

    val pipeline1 = result(
      pipelineApi.add(wkCluster.name,
                      PipelineCreationRequest(
                        name = methodName(),
                        rules = Map.empty
                      ))
    )

    pipeline1.workerClusterName shouldBe wkCluster.name

    val pipelines = result(pipelineApi.list())
    pipelines.size shouldBe 2
    pipelines.find(_.id == pipeline0.id).get.workerClusterName shouldBe pipeline0.workerClusterName
    pipelines.find(_.id == pipeline1.id).get.workerClusterName shouldBe pipeline1.workerClusterName
  }

  @Test
  def testUnmatchedId(): Unit = {
    // test invalid request: nonexistent uuid
    val invalidRequest = PipelineCreationRequest(methodName, Map("invalid" -> Seq("invalid")))
    val pipeline = result(pipelineApi.add(invalidRequest))
    pipeline.rules.size shouldBe 0
  }

  @Test
  def testNormalCase(): Unit = {
    val connector = Await.result(
      ConnectorApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          ConnectorCreationRequest(
            name = methodName(),
            className = methodName(),
            topics = Seq.empty,
            numberOfTasks = 1,
            schema = Seq.empty,
            configs = Map.empty
          )),
      10 seconds
    )

    val topic = Await.result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(TopicApi.creationRequest(methodName())),
      10 seconds
    )

    val pipeline = Await.result(
      pipelineApi.add(
        PipelineCreationRequest(
          name = methodName(),
          rules = Map(connector.id -> Seq(topic.id))
        )),
      10 seconds
    )

    pipeline.rules.size shouldBe 1
    pipeline.rules(connector.id) shouldBe Seq(topic.id)

    // remove connector
    Await.result(ConnectorApi.access().hostname(configurator.hostname).port(configurator.port).delete(connector.id),
                 10 seconds)

    val pipeline2 = Await.result(pipelineApi.get(pipeline.id), 10 seconds)

    pipeline2.rules.size shouldBe 0

    val pipelines = Await.result(pipelineApi.list(), 10 seconds)

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
    val uuid_0 = result(topicAccess.add(TopicApi.creationRequest(methodName()))).id
    val uuid_1 = result(topicAccess.add(TopicApi.creationRequest(methodName()))).id
    val uuid_2 = result(topicAccess.add(TopicApi.creationRequest(methodName()))).id

    result(pipelineApi.list()).size shouldBe 0

    val request = PipelineCreationRequest(methodName, Map(uuid_0 -> Seq(uuid_1)))
    val response = compareRequestAndResponse(request, result(pipelineApi.add(request)))

    // test get
    compare2Response(response, result(pipelineApi.get(response.id)))

    // test update
    val anotherRequest = PipelineCreationRequest(methodName, Map(uuid_0 -> Seq(uuid_2)))
    val newResponse =
      compareRequestAndResponse(anotherRequest, result(pipelineApi.update(response.id, anotherRequest)))

    // topics should have no state
    newResponse.objects.foreach(_.state shouldBe None)

    // test get
    compare2Response(newResponse, result(pipelineApi.get(newResponse.id)))

    // test delete
    result(pipelineApi.list()).size shouldBe 1
    result(pipelineApi.delete(response.id)) shouldBe newResponse
    result(pipelineApi.list()).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(pipelineApi.get("asdasdsad"))
    an[IllegalArgumentException] should be thrownBy result(pipelineApi.update("asdasdsad", anotherRequest))
  }

  @Test
  def testBindInvalidObjects2Pipeline(): Unit = {
    val topicAccess = TopicApi.access().hostname(configurator.hostname).port(configurator.port)
    val hdfsAccess = HadoopApi.access().hostname(configurator.hostname).port(configurator.port)
    val uuid_0 = result(topicAccess.add(TopicApi.creationRequest(methodName()))).id
    val uuid_1 = result(hdfsAccess.add(HdfsInfoRequest(methodName, "file:///"))).id
    val uuid_2 = result(hdfsAccess.add(HdfsInfoRequest(methodName, "file:///"))).id
    val uuid_3 = result(topicAccess.add(TopicApi.creationRequest(methodName()))).id
    result(topicAccess.list()).size shouldBe 2
    result(hdfsAccess.list()).size shouldBe 2

    // uuid_0 -> uuid_0: self-bound
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.add(PipelineCreationRequest(methodName, Map(uuid_0 -> Seq(uuid_0)))))
    // uuid_1 can't be applied to pipeline
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.add(PipelineCreationRequest(methodName, Map(uuid_0 -> Seq(uuid_1)))))
    // uuid_2 can't be applied to pipeline
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.add(PipelineCreationRequest(methodName, Map(uuid_0 -> Seq(uuid_2)))))

    val res = result(pipelineApi.add(PipelineCreationRequest(methodName, Map(uuid_0 -> Seq(uuid_3)))))
    // uuid_0 -> uuid_0: self-bound
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.update(res.id, PipelineCreationRequest(methodName, Map(uuid_0 -> Seq(uuid_0)))))
    // uuid_1 can't be applied to pipeline
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.update(res.id, PipelineCreationRequest(methodName, Map(uuid_0 -> Seq(uuid_1)))))
    // uuid_2 can't be applied to pipeline
    an[IllegalArgumentException] should be thrownBy result(
      pipelineApi.update(res.id, PipelineCreationRequest(methodName, Map(uuid_0 -> Seq(uuid_2)))))

    // good case
    result(pipelineApi.update(res.id, PipelineCreationRequest(methodName, Map(uuid_0 -> Seq(uuid_3))))).name shouldBe methodName
  }

  @Test
  def unknownKeyShouldBeFiltered(): Unit = {
    val pipeline = Await.result(
      pipelineApi.add(
        PipelineCreationRequest(
          name = methodName(),
          rules = Map(PipelineApi.UNKNOWN -> Seq("Adasd", "asdasd"))
        )),
      10 seconds
    )

    pipeline.rules.size shouldBe 0
  }

  @Test
  def unknownValueShouldBeFiltered(): Unit = {
    val topic = Await.result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(TopicApi.creationRequest(name = methodName())),
      10 seconds
    )

    val pipeline = Await.result(
      pipelineApi.add(
        PipelineCreationRequest(
          name = methodName(),
          rules = Map(topic.id -> Seq(PipelineApi.UNKNOWN, PipelineApi.UNKNOWN, PipelineApi.UNKNOWN))
        )),
      10 seconds
    )

    pipeline.rules.size shouldBe 1
    pipeline.rules(topic.id).size shouldBe 0
  }

  @Test
  def removeConnectorFromDeletedCluster(): Unit = {
    val topic = Await.result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(TopicApi.creationRequest(name = methodName())),
      10 seconds
    )

    val pipeline = result(
      pipelineApi.add(
        PipelineCreationRequest(
          name = methodName(),
          rules = Map(topic.id -> Seq(PipelineApi.UNKNOWN, PipelineApi.UNKNOWN, PipelineApi.UNKNOWN))
        )))

    val wk = result(configurator.clusterCollie.workerCollie().remove(pipeline.workerClusterName))
    wk.name shouldBe pipeline.workerClusterName

    result(pipelineApi.delete(pipeline.id))

    result(pipelineApi.list()).exists(_.id == pipeline.id) shouldBe false
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
