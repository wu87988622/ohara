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

import java.net.URL

import com.island.ohara.client.configurator.v0.JarApi.JarInfo
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.StreamApi.{
  StreamAppDescription,
  StreamClusterInfo,
  StreamJar,
  StreamListRequest,
  StreamPropertyRequest
}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, VersionUtils}
import org.junit.Test
import org.scalatest.Matchers

class TestStreamApi extends SmallTest with Matchers {

  @Test
  def checkVersion(): Unit = {
    StreamApi.IMAGE_NAME_DEFAULT shouldBe s"oharastream/streamapp:${VersionUtils.VERSION}"
  }

  @Test
  def testStreamJarEquals(): Unit = {
    val info = StreamJar(
      workerClusterName = CommonUtils.randomString(5),
      id = CommonUtils.uuid(),
      name = "test.jar",
      lastModified = CommonUtils.current()
    )

    info shouldBe StreamApi.STREAM_JAR_JSON_FORMAT.read(StreamApi.STREAM_JAR_JSON_FORMAT.write(info))
  }

  @Test
  def testStreamListRequestEquals(): Unit = {
    val info = StreamListRequest(
      jarName = "new-jar.jar"
    )

    info shouldBe StreamApi.STREAM_LIST_REQUEST_JSON_FORMAT.read(StreamApi.STREAM_LIST_REQUEST_JSON_FORMAT.write(info))
  }

  @Test
  def testStreamPropertyRequestEquals(): Unit = {
    val info = StreamPropertyRequest(
      jarId = CommonUtils.randomString(5),
      name = Some("app-id"),
      from = Some(Seq("from")),
      to = Some(Seq("to")),
      instances = Some(5)
    )

    info shouldBe StreamApi.STREAM_PROPERTY_REQUEST_JSON_FORMAT.read(
      StreamApi.STREAM_PROPERTY_REQUEST_JSON_FORMAT.write(info))
  }

  @Test
  def testStreamAppDescriptionEquals(): Unit = {
    val id = CommonUtils.uuid()
    val info = StreamAppDescription(
      workerClusterName = CommonUtils.randomString(5),
      id = id,
      name = "my-app",
      instances = 1,
      jarInfo = JarInfo("id", "name", "group", 1L, new URL("http://localshot:12345/v0"), CommonUtils.current()),
      from = Seq.empty,
      to = Seq.empty,
      state = None,
      metrics = Metrics(Seq.empty),
      error = None,
      lastModified = CommonUtils.current()
    )

    info shouldBe StreamApi.STREAMAPP_DESCRIPTION_JSON_FORMAT.read(
      StreamApi.STREAMAPP_DESCRIPTION_JSON_FORMAT.write(info))
  }

  @Test
  def testPortsShouldBeEmpty(): Unit = {
    val info = StreamClusterInfo(
      name = "foo",
      imageName = "bar",
      nodeNames = Seq("fake"),
      jmxPort = 999,
      state = Some("RUNNING")
    )

    info.name shouldBe "foo"
    info.imageName shouldBe "bar"
    info.nodeNames.size shouldBe 1
    info.nodeNames.head shouldBe "fake"
    info.jmxPort shouldBe 999
    info.state.get shouldBe "RUNNING"
    info.ports shouldBe Set.empty
  }
}
