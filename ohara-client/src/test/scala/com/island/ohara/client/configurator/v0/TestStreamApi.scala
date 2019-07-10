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

import com.island.ohara.client.configurator.v0.JarApi.JarKey
import com.island.ohara.client.configurator.v0.MetricsApi.Metrics
import com.island.ohara.client.configurator.v0.StreamApi.{Creation, StreamAppDescription, StreamClusterInfo}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, VersionUtils}
import org.junit.Test
import org.scalatest.Matchers
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestStreamApi extends SmallTest with Matchers {

  private[this] final val propertyAccess =
    StreamApi.accessOfProperty.hostname(CommonUtils.randomString()).port(CommonUtils.availablePort())
  private[this] final val fakeJar = JarKey(CommonUtils.randomString(1), CommonUtils.randomString(1))
  private[this] final def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def checkVersion(): Unit = {
    StreamApi.IMAGE_NAME_DEFAULT shouldBe s"oharastream/streamapp:${VersionUtils.VERSION}"
  }

  @Test
  def testCloneNodeNames(): Unit = {
    val newNodeNames = Set(CommonUtils.randomString())
    val info = StreamClusterInfo(
      name = CommonUtils.randomString(10),
      imageName = CommonUtils.randomString(),
      jmxPort = 10,
      nodeNames = Set.empty,
      deadNodes = Set.empty
    )
    info.clone(newNodeNames).nodeNames shouldBe newNodeNames
  }

  @Test
  def testStreamPropertyRequestEquals(): Unit = {
    val info = Creation(
      imageName = "image",
      jar = JarKey("group", "name"),
      name = "appid",
      from = Set("from"),
      to = Set("to"),
      jmxPort = 5555,
      instances = 1,
      nodeNames = Set("node1"),
      tags = Set.empty
    )

    info shouldBe StreamApi.STREAM_CREATION_JSON_FORMAT.read(StreamApi.STREAM_CREATION_JSON_FORMAT.write(info))
  }

  @Test
  def testStreamAppDescriptionEquals(): Unit = {
    val info = StreamAppDescription(
      name = "my-app",
      imageName = "image",
      instances = 1,
      deadNodes = Set.empty,
      nodeNames = Set("node1"),
      jar = JarKey("group", "name"),
      from = Set.empty,
      to = Set.empty,
      state = None,
      jmxPort = 0,
      metrics = Metrics(Seq.empty),
      error = None,
      lastModified = CommonUtils.current(),
      tags = Set.empty
    )

    info shouldBe StreamApi.STREAMAPP_DESCRIPTION_JSON_FORMAT.read(
      StreamApi.STREAMAPP_DESCRIPTION_JSON_FORMAT.write(info))
  }

  @Test
  def nameFieldCheck(): Unit = {
    an[NullPointerException] should be thrownBy propertyAccess.request.name(null)
    an[IllegalArgumentException] should be thrownBy propertyAccess.request.name("")
  }

  @Test
  def imageNameFieldCheck(): Unit = {
    an[NullPointerException] should be thrownBy propertyAccess.request.imageName(null)
    an[IllegalArgumentException] should be thrownBy propertyAccess.request.imageName("")

    // default value
    propertyAccess.request
      .name(CommonUtils.randomString())
      .jar(fakeJar)
      .creation
      .imageName shouldBe StreamApi.IMAGE_NAME_DEFAULT
  }

  @Test
  def jarFieldCheck(): Unit = {
    an[NullPointerException] should be thrownBy propertyAccess.request.imageName(null)
    an[IllegalArgumentException] should be thrownBy propertyAccess.request.imageName("")
  }

  @Test
  def topicFromFieldCheck(): Unit = {
    an[NullPointerException] should be thrownBy propertyAccess.request.from(null)
    an[IllegalArgumentException] should be thrownBy propertyAccess.request.from(Set.empty)

    // default value
    propertyAccess.request.name(CommonUtils.randomString()).jar(fakeJar).creation.from shouldBe Set.empty
  }

  @Test
  def topicToFieldCheck(): Unit = {
    an[NullPointerException] should be thrownBy propertyAccess.request.to(null)
    an[IllegalArgumentException] should be thrownBy propertyAccess.request.to(Set.empty)

    // default value
    propertyAccess.request.name(CommonUtils.randomString()).jar(fakeJar).creation.to shouldBe Set.empty
  }

  @Test
  def jmxPortFieldCheck(): Unit = {
    an[IllegalArgumentException] should be thrownBy propertyAccess.request.jmxPort(0)
    an[IllegalArgumentException] should be thrownBy propertyAccess.request.jmxPort(-1)

    // default value
    CommonUtils.requireConnectionPort(
      propertyAccess.request.name(CommonUtils.randomString()).jar(fakeJar).creation.jmxPort)
  }

  @Test
  def instancesFieldCheck(): Unit = {
    an[IllegalArgumentException] should be thrownBy propertyAccess.request.instances(0)
    an[IllegalArgumentException] should be thrownBy propertyAccess.request.instances(-1)

    // default value
    propertyAccess.request.name(CommonUtils.randomString()).jar(fakeJar).creation.instances shouldBe 1
  }

  @Test
  def nodeNamesFieldCheck(): Unit = {
    an[NullPointerException] should be thrownBy propertyAccess.request.nodeNames(null)
    an[IllegalArgumentException] should be thrownBy propertyAccess.request.nodeNames(Set.empty)

    // default value
    propertyAccess.request.name(CommonUtils.randomString()).jar(fakeJar).creation.nodeNames shouldBe Set.empty
  }

  @Test
  def requireFieldOnPropertyCreation(): Unit =
    // jar is required
    an[NullPointerException] should be thrownBy propertyAccess.request.name(CommonUtils.randomString()).creation

  @Test
  def testMinimumCreation(): Unit = {
    val name = CommonUtils.randomString(10)
    val creation = propertyAccess.request.name(name).jar(fakeJar).creation

    creation.name shouldBe name
    creation.imageName shouldBe StreamApi.IMAGE_NAME_DEFAULT
    creation.jar shouldBe fakeJar
    creation.from shouldBe Set.empty
    creation.to shouldBe Set.empty
    creation.jmxPort should not be 0
    creation.instances shouldBe 1
    creation.nodeNames shouldBe Set.empty
  }

  @Test
  def testCreation(): Unit = {
    val name = CommonUtils.randomString(10)
    val imageName = CommonUtils.randomString()
    val from = Set(CommonUtils.randomString())
    val to = Set(CommonUtils.randomString())
    val jmxPort = CommonUtils.availablePort()
    val instances = CommonUtils.randomString().length
    val nodeNames = Set(CommonUtils.randomString())
    val creation = propertyAccess.request
      .name(name)
      .imageName(imageName)
      .jar(fakeJar)
      .from(from)
      .to(to)
      .jmxPort(jmxPort)
      .instances(instances)
      .nodeNames(nodeNames)
      .creation

    creation.name shouldBe name
    creation.imageName shouldBe imageName
    creation.jar shouldBe fakeJar
    creation.from shouldBe from
    creation.to shouldBe to
    creation.jmxPort shouldBe jmxPort
    creation.instances shouldBe instances
    creation.nodeNames shouldBe nodeNames
  }

  @Test
  def parseCreation(): Unit = {
    val from = "from"
    val to = "to"
    val nodeName = "n0"
    val creation = StreamApi.STREAM_CREATION_JSON_FORMAT.read(s"""
                                                                  |  {
                                                                  |    "from": ["$from"],
                                                                  |    "to": ["$to"],
                                                                  |    "nodeNames": ["$nodeName"],
                                                                  |    "jar": ${fakeJar.toJson}
                                                                  |  }
           """.stripMargin.parseJson)
    creation.name.length shouldBe StreamApi.LIMIT_OF_NAME_LENGTH
    creation.imageName shouldBe StreamApi.IMAGE_NAME_DEFAULT
    creation.jar shouldBe fakeJar
    creation.from shouldBe Set(from)
    creation.to shouldBe Set(to)
    creation.jmxPort should not be 0
    creation.instances shouldBe 1
    creation.nodeNames shouldBe Set(nodeName)

    val name = CommonUtils.randomString(10)
    val creation2 = StreamApi.STREAM_CREATION_JSON_FORMAT.read(s"""
                                                                 |  {
                                                                 |    "name": "$name",
                                                                 |    "from": ["$from"],
                                                                 |    "to": ["$to"],
                                                                 |    "nodeNames": ["$nodeName"],
                                                                 |    "jar": ${fakeJar.toJson}
                                                                 |  }
           """.stripMargin.parseJson)

    creation2.name shouldBe name
    creation2.imageName shouldBe StreamApi.IMAGE_NAME_DEFAULT
    creation2.jar shouldBe fakeJar
    creation.from shouldBe Set(from)
    creation.to shouldBe Set(to)
    creation2.jmxPort should not be 0
    creation2.instances shouldBe 1
    creation.nodeNames shouldBe Set(nodeName)
  }

  @Test
  def parseNameField(): Unit = {
    val thrown2 = the[DeserializationException] thrownBy StreamApi.STREAM_CREATION_JSON_FORMAT.read(s"""
                                                  |  {
                                                  |    "name": "",
                                                  |    "jar": ${fakeJar.toJson}
                                                  |  }
           """.stripMargin.parseJson)
    thrown2.getMessage should include("the value of \"name\" can't be empty string")
  }

  @Test
  def parseImageNameField(): Unit = {
    val name = CommonUtils.randomString(10)
    val thrown = the[DeserializationException] thrownBy StreamApi.STREAM_CREATION_JSON_FORMAT.read(s"""
                                                                                                  |  {
                                                                                                  |    "name": "$name",
                                                                                                  |    "jar": ${fakeJar.toJson},
                                                                                                  |    "imageName": ""
                                                                                                  |  }
           """.stripMargin.parseJson)
    thrown.getMessage should include("the value of \"imageName\" can't be empty string")
  }

  @Test
  def parseJarField(): Unit = {
    val name = CommonUtils.randomString(10)

    // jar is required
    val thrown1 = the[DeserializationException] thrownBy StreamApi.STREAM_CREATION_JSON_FORMAT.read(s"""
                                                                                                  |  {
                                                                                                  |    "name": "$name"
                                                                                                  |  }
           """.stripMargin.parseJson)
    thrown1.getMessage should include("Object is missing required member 'jar'")

    val thrown2 = the[DeserializationException] thrownBy StreamApi.STREAM_CREATION_JSON_FORMAT.read(s"""
                                                                                                  |  {
                                                                                                  |    "name": "$name",
                                                                                                  |    "jar": ""
                                                                                                  |  }
           """.stripMargin.parseJson)
    thrown2.getMessage should include("the value of \"jar\" can't be empty string")
  }

  @Test
  def parseJmxPortField(): Unit = {
    // zero port
    val thrown1 = the[DeserializationException] thrownBy StreamApi.STREAM_CREATION_JSON_FORMAT.read(s"""
          |  {
          |    "name": "${CommonUtils.randomString(10)}",
          |    "jar": ${fakeJar.toJson},
          |    "jmxPort": 0
          |  }
           """.stripMargin.parseJson)
    thrown1.getMessage should include("the connection port must be [1024, 65535)")

    // negative port
    val thrown2 = the[DeserializationException] thrownBy StreamApi.STREAM_CREATION_JSON_FORMAT.read(s"""
                        |  {
                        |    "name": "${CommonUtils.randomString(10)}",
                        |    "jar": ${fakeJar.toJson},
                        |    "jmxPort": -99
                        |  }
           """.stripMargin.parseJson)
    thrown2.getMessage should include("the value of \"jmxPort\" MUST be bigger than or equal to zero")

    // not connection port
    val thrown3 = the[DeserializationException] thrownBy StreamApi.STREAM_CREATION_JSON_FORMAT.read(s"""
        |  {
        |    "name": "${CommonUtils.randomString(10)}",
        |    "jar": ${fakeJar.toJson},
        |    "jmxPort": 999999
        |  }
           """.stripMargin.parseJson)
    thrown3.getMessage should include("the connection port must be [1024, 65535)")
  }

  @Test
  def parseInstancesField(): Unit = {
    // zero instances is ok since we still check this value must bigger than 0 in streamRoute
    StreamApi.STREAM_CREATION_JSON_FORMAT.read(s"""
                                                  |  {
                                                  |    "name": "${CommonUtils.randomString(10)}",
                                                  |    "jar": ${fakeJar.toJson},
                                                  |    "instances": 0
                                                  |  }
           """.stripMargin.parseJson)
    // negative instances
    val thrown = the[DeserializationException] thrownBy StreamApi.STREAM_CREATION_JSON_FORMAT.read(s"""
                                                                                                  |  {
                                                                                                  |    "name": "${CommonUtils
                                                                                                        .randomString(
                                                                                                          10)}",
                                                                                                  |    "jar": ${fakeJar.toJson},
                                                                                                  |    "instances": -99
                                                                                                  |  }
           """.stripMargin.parseJson)
    thrown.getMessage should include("the value of \"instances\" MUST be bigger than or equal to zero")
  }

  @Test
  def requireFieldOnPropertyUpdate(): Unit = {
    // name is required
    an[NullPointerException] should be thrownBy result(propertyAccess.request.jar(JarKey("group", "name")).update())

    // no jar is ok
    propertyAccess.request.name(CommonUtils.randomString()).update
  }

  @Test
  def testDefaultUpdate(): Unit = {
    val name = CommonUtils.randomString(10)
    val data = propertyAccess.request.name(name).update
    data.imageName.isEmpty shouldBe true
    data.from.isEmpty shouldBe true
    data.to.isEmpty shouldBe true
    data.jmxPort.isEmpty shouldBe true
    data.instances.isEmpty shouldBe true
    data.nodeNames.isEmpty shouldBe true
  }

  @Test
  def parseMinimumJsonUpdate(): Unit = {
    val name = CommonUtils.randomString(10)
    val data = StreamApi.STREAM_UPDATE_JSON_FORMAT.read(s"""
                                                               |  {
                                                               |    "name": "$name",
                                                               |    "jar": ${fakeJar.toJson}
                                                               |  }
           """.stripMargin.parseJson)

    data.imageName.isEmpty shouldBe true
    data.from.isEmpty shouldBe true
    data.to.isEmpty shouldBe true
    data.jmxPort.isEmpty shouldBe true
    data.instances.isEmpty shouldBe true
    data.nodeNames.isEmpty shouldBe true
  }

  @Test
  def parseImageNameFieldOnUpdate(): Unit = {
    val thrown = the[DeserializationException] thrownBy StreamApi.STREAM_UPDATE_JSON_FORMAT.read(s"""
                                                                                                |  {
                                                                                                |    "imageName": ""
                                                                                                |  }
           """.stripMargin.parseJson)
    thrown.getMessage should include("the value of \"imageName\" can't be empty string")
  }

  @Test
  def parseFromFieldOnCreation(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy StreamApi.STREAM_CREATION_JSON_FORMAT.read(s"""
                                                                                                     |  {
                                                                                                     |    "from": "",
                                                                                                     |    "jar": ${fakeJar.toJson}
                                                                                                     |  }
           """.stripMargin.parseJson)
    thrown1.getMessage should include("the value of \"from\" can't be empty string")
  }

  @Test
  def parseFromFieldOnUpdate(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy StreamApi.STREAM_UPDATE_JSON_FORMAT.read(s"""
                                                                                                |  {
                                                                                                |    "from": ""
                                                                                                |  }
           """.stripMargin.parseJson)
    thrown1.getMessage should include("the value of \"from\" can't be empty string")
  }

  @Test
  def parseToFieldOnCreation(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy StreamApi.STREAM_CREATION_JSON_FORMAT.read(s"""
                                                                                                     |  {
                                                                                                     |    "to": "",
                                                                                                     |    "jar": ${fakeJar.toJson}
                                                                                                     |  }
           """.stripMargin.parseJson)
    thrown1.getMessage should include("the value of \"to\" can't be empty string")
  }

  @Test
  def parseToFieldOnUpdate(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy StreamApi.STREAM_UPDATE_JSON_FORMAT.read(s"""
                                                                                                |  {
                                                                                                |    "to": ""
                                                                                                |  }
           """.stripMargin.parseJson)
    thrown1.getMessage should include("the value of \"to\" can't be empty string")
  }

  @Test
  def parseJmxPortFieldOnUpdate(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy StreamApi.STREAM_UPDATE_JSON_FORMAT.read(s"""
                                                                                                |  {
                                                                                                |    "jmxPort": 0
                                                                                                |  }
           """.stripMargin.parseJson)
    thrown1.getMessage should include("the connection port must be [1024, 65535)")

    val thrown2 = the[DeserializationException] thrownBy StreamApi.STREAM_UPDATE_JSON_FORMAT.read(s"""
                                                                                                |  {
                                                                                                |    "jmxPort": -9
                                                                                                |  }
           """.stripMargin.parseJson)
    thrown2.getMessage should include("the value of \"jmxPort\" MUST be bigger than or equal to zero")

    val thrown3 = the[DeserializationException] thrownBy StreamApi.STREAM_UPDATE_JSON_FORMAT.read(s"""
                                                                                                |  {
                                                                                                |    "jmxPort": 99999
                                                                                                |  }
           """.stripMargin.parseJson)
    thrown3.getMessage should include("the connection port must be [1024, 65535)")
  }

  @Test
  def parseInstancesFieldOnUpdate(): Unit = {
    // zero instances is ok since we still check this value must bigger than 0 in streamRoute
    StreamApi.STREAM_UPDATE_JSON_FORMAT.read(s"""
                                                |  {
                                                |    "name": "${CommonUtils.randomString()}",
                                                |    "jar": ${fakeJar.toJson},
                                                |    "instances": 0
                                                |  }
           """.stripMargin.parseJson)

    val thrown = the[DeserializationException] thrownBy StreamApi.STREAM_UPDATE_JSON_FORMAT.read(s"""
                                                                                                |  {
                                                                                                |    "instances": -9
                                                                                                |  }
           """.stripMargin.parseJson)
    thrown.getMessage should include("the value of \"instances\" MUST be bigger than or equal to zero")
  }

  @Test
  def parseNodeNamesFieldOnCreation(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy StreamApi.STREAM_CREATION_JSON_FORMAT.read(s"""
                                                                                                     |  {
                                                                                                     |    "nodeNames": "",
                                                                                                     |    "jar": ${fakeJar.toJson}
                                                                                                     |  }
           """.stripMargin.parseJson)
    thrown1.getMessage should include("the value of \"nodeNames\" can't be empty string")
  }

  @Test
  def parseNodeNamesFieldOnUpdate(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy StreamApi.STREAM_UPDATE_JSON_FORMAT.read(s"""
                                                                                                |  {
                                                                                                |    "nodeNames": ""
                                                                                                |  }
           """.stripMargin.parseJson)
    thrown1.getMessage should include("the value of \"nodeNames\" can't be empty string")
  }

  @Test
  def ignoreNameOnCreation(): Unit = StreamApi.accessOfProperty
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .jar(JarKey(group = "1", name = "b"))
    .creation
    .name
    .length should not be 0
}
