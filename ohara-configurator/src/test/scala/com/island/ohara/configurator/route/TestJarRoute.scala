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

import java.io.{File, FileOutputStream}
import java.util.concurrent.TimeUnit

import com.island.ohara.client.configurator.v0.JarApi.JarKey
import com.island.ohara.client.configurator.v0.{JarApi, StreamApi}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
class TestJarRoute extends SmallTest with Matchers {

  private[this] var configurator: Configurator = _
  private[this] var accessStream: StreamApi.Access = _
  private[this] var jarApi: JarApi.Access = _
  private[this] val GROUP = s"group-${this.getClass.getSimpleName}"

  private[this] def tmpFile(bytes: Array[Byte]): File = {
    val f = CommonUtils.createTempJar(methodName())
    val output = new FileOutputStream(f)
    try output.write(bytes)
    finally output.close()
    f
  }

  @Before
  def setup(): Unit = {
    configurator = Configurator.builder.fake().build()
    accessStream = StreamApi.access.hostname(configurator.hostname).port(configurator.port)
    jarApi = JarApi.access.hostname(configurator.hostname).port(configurator.port)
  }

  @Test
  def testUpload(): Unit = {
    // upload jar to random group
    val data = methodName().getBytes
    val f = tmpFile(data)
    val jar = result(jarApi.request.upload(f))
    jar.size shouldBe f.length()
    f.getName.contains(jar.name) shouldBe true
    result(jarApi.request.list()).size shouldBe 1

    // upload jar to specific group
    val jarWithGroup = result(jarApi.request.group(GROUP).upload(f))
    jarWithGroup.group shouldBe GROUP
    jarWithGroup.size shouldBe data.size

    // since name == name, use same upload jar will get same name
    jarWithGroup.name shouldBe jar.name

    f.deleteOnExit()
  }

  @Test
  def testUploadOutOfLimitFile(): Unit = {
    val bytes = new Array[Byte](RouteUtils.DEFAULT_JAR_SIZE_BYTES.toInt + 1)
    val f = tmpFile(bytes)

    an[IllegalArgumentException] should be thrownBy result(jarApi.request.upload(f))

    f.deleteOnExit()
  }

  @Test
  def testUploadWithNewName(): Unit = {
    val data = methodName().getBytes
    val f = tmpFile(data)
    val jar1 = result(jarApi.request.newName("xxxx").upload(f))
    jar1.size shouldBe f.length()
    jar1.name shouldBe "xxxx"
    result(jarApi.request.list()).size shouldBe 1

    val jar2 = result(jarApi.request.newName("yyyy").upload(f))
    jar1.name should not be jar2.name

    val jar3 = result(jarApi.request.newName("xxxx").group(GROUP).upload(f))
    val jar4 = result(jarApi.request.newName("yyyy").group(GROUP).upload(f))
    result(jarApi.request.list()).size shouldBe 4
    jar3.group shouldBe GROUP
    jar4.group shouldBe GROUP
    jar3.name should not be jar4.name

    f.deleteOnExit()
  }

  @Test
  def testUploadSameNameFile(): Unit = {
    val data = methodName().getBytes
    val f = tmpFile(data)

    val jar1 = result(jarApi.request.newName("barfoo.jar").upload(f))
    TimeUnit.SECONDS.sleep(3)
    f.setLastModified(System.currentTimeMillis())
    val jar2 = result(jarApi.request.newName("barfoo.jar").upload(f))

    val jar3 = result(jarApi.request.newName("barfoo.jar").group(GROUP).upload(f))
    TimeUnit.SECONDS.sleep(3)
    f.setLastModified(System.currentTimeMillis())
    val jar4 = result(jarApi.request.newName("barfoo.jar").group(GROUP).upload(f))

    // will get latest modified file of same name files
    // different group means different files (even if same name)
    val res = result(jarApi.request.list())
    res.size shouldBe 3
    res.contains(jar1) shouldBe true
    res.contains(jar2) shouldBe true
    res.contains(jar3) shouldBe false
    res.contains(jar4) shouldBe true

    f.deleteOnExit()
  }

  @Test
  def testDelete(): Unit = {
    val data = methodName().getBytes
    val f = tmpFile(data)
    val jar = result(jarApi.request.upload(f))
    jar.size shouldBe f.length()
    f.getName.contains(jar.name) shouldBe true
    result(jarApi.request.list()).size shouldBe 1

    result(jarApi.request.group(jar.group).delete(jar.name))
    result(jarApi.request.list()).size shouldBe 0

    f.deleteOnExit()
  }

  @Test
  def testDeleteJarUsedInStreamApp(): Unit = {
    val data = methodName().getBytes
    val name = CommonUtils.randomString(10)
    val f = tmpFile(data)
    // upload jar
    val jar = result(jarApi.request.upload(f))
    // create streamApp property
    result(accessStream.request.name(name).jar(JarKey(jar.group, jar.name)).create())
    // cannot delete a used jar
    val thrown = the[IllegalArgumentException] thrownBy result(jarApi.request.group(jar.group).delete(jar.name))
    thrown.getMessage should include("in used")

    result(accessStream.delete(name))
    // delete is ok after remove property
    result(jarApi.request.group(jar.group).delete(jar.name))

    // the jar should be disappear
    val thrown1 = the[IllegalArgumentException] thrownBy result(jarApi.request.group(jar.group).get(jar.name))
    thrown1.getMessage should include("not found")

    f.deleteOnExit()
  }

  @Test
  def duplicateDeleteStreamProperty(): Unit =
    (0 to 10).foreach(_ =>
      result(jarApi.request.group(CommonUtils.randomString(5)).delete(CommonUtils.randomString(5))))

  @After
  def tearDown(): Unit = Releasable.close(configurator)

}
