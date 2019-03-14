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

package com.island.ohara.it.streams

import java.io.File

import com.island.ohara.client.configurator.v0.ContainerApi.ContainerState
import com.island.ohara.client.configurator.v0.StreamApi.StreamPropertyRequest
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.client.configurator.v0.{StreamApi, TopicApi}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import com.island.ohara.it.IntegrationTest
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

class TestRunStreamApp extends IntegrationTest with Matchers {
  private[this] val configurator =
    Configurator.builder().advertisedHostname(CommonUtils.hostname()).advertisedPort(0).fake().build()

  private[this] val pipeline_id = "pipeline-id"
  private[this] val instances = 1

  private[this] val streamAppActionAccess =
    StreamApi.accessOfAction().hostname(configurator.hostname).port(configurator.port)
  private[this] val streamAppListAccess =
    StreamApi.accessOfList().hostname(configurator.hostname).port(configurator.port)
  private[this] val streamAppPropertyAccess =
    StreamApi.accessOfProperty().hostname(configurator.hostname).port(configurator.port)

  @Before
  def setup(): Unit = {
    //TODO remove this line after OHARA-1528 finished
    skipTest(s"this test cannot be running inside containerize env ; will be fixed in OHARA-1528")
  }

  @Test
  def testRunSimpleStreamApp(): Unit = {
    val from = "fromTopic"
    val to = "toTopic"

    result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(from),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None))
    )
    result(
      TopicApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .add(
          TopicCreationRequest(name = Some(to),
                               brokerClusterName = None,
                               numberOfPartitions = None,
                               numberOfReplications = None))
    )

    val userJarPath =
      s"${System.getProperty("user.dir")}${File.separator}build${File.separator}resources${File.separator}test"
    val filePaths =
      new File(userJarPath)
        .listFiles()
        .filter(f => f.isFile && f.getName.endsWith("tests.jar"))
        .map(f => f.getPath)
        .toSeq

    // Upload Jar
    val jarData = result(
      streamAppListAccess.upload(pipeline_id, filePaths)
    )

    //Get topic information
    val topicInfos = result(
      TopicApi.access().hostname(configurator.hostname).port(configurator.port).list()
    )
    val fromTopic = topicInfos
      .filter { info =>
        info.name == from
      }
      .map(_.id)
      .head
    val toTopic = topicInfos
      .filter { info =>
        info.name == to
      }
      .map(_.id)
      .head

    // Update StreamApp Properties
    val req = StreamPropertyRequest(
      CommonUtils.randomString(10),
      Seq(fromTopic),
      Seq(toTopic),
      instances
    )
    val streamAppProp = result(
      streamAppPropertyAccess.update(jarData.head.id, req)
    )
    streamAppProp.fromTopics.size shouldBe 1
    streamAppProp.toTopics.size shouldBe 1
    streamAppProp.instances shouldBe 1

    //Start StreamApp
    val res1 =
      result(streamAppActionAccess.start(jarData.head.id))
    res1.id shouldBe jarData.head.id
    res1.state.getOrElse("") shouldBe ContainerState.RUNNING

    //Stop StreamApp
    val res2 =
      result(streamAppActionAccess.stop(jarData.head.id))
    res2.state.isEmpty shouldBe true
  }

  @After
  def tearDown(): Unit = {
    Releasable.close(configurator)
  }
}
