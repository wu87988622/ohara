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

import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterCreationRequest
import com.island.ohara.client.configurator.v0.TopicApi._
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterCreationRequest
import com.island.ohara.client.configurator.v0.{BrokerApi, TopicApi, ZookeeperApi}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
class TestTopicRoute extends SmallTest with Matchers {

  private[this] val configurator = Configurator.builder().fake(1, 0).build()

  private[this] val topicApi = TopicApi.access().hostname(configurator.hostname).port(configurator.port)

  @Test
  def test(): Unit = {
    def compareRequestAndResponse(request: TopicCreationRequest, response: TopicInfo): TopicInfo = {
      request.name.foreach(_ shouldBe response.name)
      request.numberOfReplications.foreach(_ shouldBe response.numberOfReplications)
      request.numberOfPartitions.foreach(_ shouldBe response.numberOfPartitions)
      response
    }

    def compare2Response(lhs: TopicInfo, rhs: TopicInfo): Unit = {
      lhs.id shouldBe rhs.id
      lhs.name shouldBe rhs.name
      lhs.numberOfReplications shouldBe rhs.numberOfReplications
      lhs.numberOfPartitions shouldBe rhs.numberOfPartitions
      lhs.lastModified shouldBe rhs.lastModified
    }

    // test add
    result(topicApi.list).size shouldBe 0
    val request = TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                                       brokerClusterName = None,
                                       numberOfPartitions = None,
                                       numberOfReplications = None)
    val response = compareRequestAndResponse(request, result(topicApi.add(request)))

    // test get
    compare2Response(response, result(topicApi.get(response.id)))

    // test update
    val anotherRequest = TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                                              brokerClusterName = None,
                                              numberOfPartitions = None,
                                              numberOfReplications = None)
    val newResponse =
      compareRequestAndResponse(anotherRequest, result(topicApi.update(response.id, anotherRequest)))

    // test get
    compare2Response(newResponse, result(topicApi.get(newResponse.id)))

    // test delete
    result(topicApi.list).size shouldBe 1
    result(topicApi.delete(response.id))
    result(topicApi.list).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(topicApi.get("123"))
    an[IllegalArgumentException] should be thrownBy result(topicApi.update("777", anotherRequest))

    // test same name
    val topicNames: Set[String] =
      (0 until 5)
        .map(
          index =>
            result(
              topicApi.add(
                TopicCreationRequest(name = Some(s"topic-$index"),
                                     brokerClusterName = None,
                                     numberOfPartitions = None,
                                     numberOfReplications = None))).name)
        .toSet
    topicNames.size shouldBe 5
  }

  @Test
  def removeTopicFromNonexistentBrokerCluster(): Unit = {
    val name = methodName()
    result(
      topicApi
        .add(
          TopicCreationRequest(name = Some(name),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None))
        .flatMap { topicInfo =>
          BrokerApi
            .access()
            .hostname(configurator.hostname)
            .port(configurator.port)
            .delete(topicInfo.brokerClusterName)
            .flatMap(_ => topicApi.delete(topicInfo.id))
        }
        .flatMap(_ => topicApi.list)
        .map(topics => topics.exists(_.name == name))) shouldBe false
  }

  @Test
  def createTopicOnNonexistentCluster(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      topicApi.add(
        TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                             brokerClusterName = Some(CommonUtils.randomString(10)),
                             numberOfPartitions = None,
                             numberOfReplications = None)))
  }

  @Test
  def addWithName(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(
      topicApi.add(
        TopicCreationRequest(name = None,
                             brokerClusterName = None,
                             numberOfPartitions = None,
                             numberOfReplications = None)))
  }

  @Test
  def createTopicWithoutBrokerClusterName(): Unit = {
    val zk = result(ZookeeperApi.access().hostname(configurator.hostname).port(configurator.port).list).head

    val zk2 = result(
      ZookeeperApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(ZookeeperClusterCreationRequest(
          name = CommonUtils.randomString(10),
          imageName = None,
          clientPort = Some(CommonUtils.availablePort()),
          electionPort = Some(CommonUtils.availablePort()),
          peerPort = Some(CommonUtils.availablePort()),
          nodeNames = zk.nodeNames
        )))

    val bk2 = result(
      BrokerApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(BrokerClusterCreationRequest(
          name = CommonUtils.randomString(10),
          imageName = None,
          zookeeperClusterName = Some(zk2.name),
          exporterPort = None,
          jmxPort = None,
          clientPort = Some(123),
          nodeNames = zk2.nodeNames
        )))

    an[IllegalArgumentException] should be thrownBy result(
      topicApi.add(
        TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                             brokerClusterName = None,
                             numberOfPartitions = None,
                             numberOfReplications = None)))

    topicApi.add(
      TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                           brokerClusterName = Some(bk2.name),
                           numberOfPartitions = None,
                           numberOfReplications = None))
  }

  @Test
  def testPartitions(): Unit = {
    val topic0 = result(
      topicApi.add(
        TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                             brokerClusterName = None,
                             numberOfPartitions = None,
                             numberOfReplications = None)))

    // we can't reduce number of partitions
    an[IllegalArgumentException] should be thrownBy result(
      topicApi.update(topic0.id,
                      TopicCreationRequest(name = None,
                                           brokerClusterName = None,
                                           numberOfPartitions = Some(topic0.numberOfPartitions - 1),
                                           numberOfReplications = None)))

    val topic1 = result(
      topicApi.update(topic0.id,
                      TopicCreationRequest(name = None,
                                           brokerClusterName = None,
                                           numberOfPartitions = Some(topic0.numberOfPartitions + 1),
                                           numberOfReplications = None)))

    topic0.id shouldBe topic1.id
    topic0.name shouldBe topic1.name
    topic0.numberOfPartitions + 1 shouldBe topic1.numberOfPartitions
    topic0.numberOfReplications shouldBe topic1.numberOfReplications
  }

  @Test
  def testReplications(): Unit = {
    val topic0 = result(
      topicApi.add(
        TopicCreationRequest(name = Some(CommonUtils.randomString(10)),
                             brokerClusterName = None,
                             numberOfPartitions = None,
                             numberOfReplications = None)))

    // we can't reduce number of replications
    an[IllegalArgumentException] should be thrownBy result(
      topicApi.update(
        topic0.id,
        TopicCreationRequest(name = None,
                             brokerClusterName = None,
                             numberOfPartitions = None,
                             numberOfReplications = Some((topic0.numberOfReplications - 1).asInstanceOf[Short]))
      ))

    // we can't add number of replications
    an[IllegalArgumentException] should be thrownBy result(
      topicApi.update(
        topic0.id,
        TopicCreationRequest(name = None,
                             brokerClusterName = None,
                             numberOfPartitions = None,
                             numberOfReplications = Some((topic0.numberOfReplications + 1).asInstanceOf[Short]))
      ))

    // pass since we don't make changes on number of replications
    result(
      topicApi.update(topic0.id,
                      TopicCreationRequest(name = None,
                                           brokerClusterName = None,
                                           numberOfPartitions = None,
                                           numberOfReplications = Some(topic0.numberOfReplications))))
  }

  @Test
  def duplicateDeleteStreamProperty(): Unit =
    (0 to 10).foreach(_ => result(topicApi.delete(CommonUtils.randomString(5))))

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
