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

import com.island.ohara.client.configurator.v0.StreamApi.StreamClusterInfo
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.VersionUtils
import org.junit.Test
import org.scalatest.Matchers

import scala.util.Random

class TestStreamApi extends SmallTest with Matchers {

  @Test
  def checkVersion(): Unit = {
    StreamApi.IMAGE_NAME_DEFAULT shouldBe s"oharastream/streamapp:${VersionUtils.VERSION}"
  }

  @Test
  def checkAppIdLength(): Unit = {

    val appId = Random.alphanumeric.take(StreamApi.LIMIT_OF_DOCKER_NAME_LENGTH).mkString

    an[IllegalArgumentException] should be thrownBy StreamApi.formatAppId(appId)
  }

  @Test
  def checkClusterNameChar(): Unit = {
    val appId = "this!@is#_not@@allow))string"
    an[IllegalArgumentException] should be thrownBy StreamApi.formatClusterName(appId)
  }

  @Test
  def checkPortsShouldBeEmpty(): Unit = {
    val info = StreamClusterInfo(
      name = "foo",
      imageName = "bar",
      nodeNames = Seq("fake"),
      state = Some("RUNNING")
    )

    info.name shouldBe "foo"
    info.imageName shouldBe "bar"
    info.nodeNames.size shouldBe 1
    info.nodeNames.head shouldBe "fake"
    info.state.get shouldBe "RUNNING"
    info.ports shouldBe Set.empty
  }
}
