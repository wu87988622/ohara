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

import com.island.ohara.client.configurator.v0.{JarApi, StreamApi}
import com.island.ohara.client.configurator.v0.StreamApi.StreamPropertyRequest
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
class TestJarRoute extends SmallTest with Matchers {

  private[this] val configurator = Configurator.builder().fake().build()
  private[this] val accessStreamList =
    StreamApi.accessOfList().hostname(configurator.hostname).port(configurator.port)
  private[this] val accessStreamProperty =
    StreamApi.accessOfProperty().hostname(configurator.hostname).port(configurator.port)
  private[this] val jarApi = JarApi.access().hostname(configurator.hostname).port(configurator.port)
  private[this] val GROUP = s"group-${this.getClass.getSimpleName}"

  private[this] def tmpFile(bytes: Array[Byte]): File = {
    val f = File.createTempFile(methodName(), null)
    val output = new FileOutputStream(f)
    try output.write(bytes)
    finally output.close()
    f
  }
  @Test
  def testUpload(): Unit = {
    // upload jar to random group
    val data = methodName().getBytes
    val f = tmpFile(data)
    val jar = result(jarApi.upload(f, None))
    jar.size shouldBe f.length()
    jar.name shouldBe f.getName
    result(jarApi.list).size shouldBe 1

    // we should have a random group
    result(jarApi.get(jar.id)).group.nonEmpty shouldBe true

    // upload jar to specific group
    val jarWithGroup = result(jarApi.upload(f, Some(GROUP)))
    jarWithGroup.group shouldBe GROUP
    jarWithGroup.size shouldBe data.size
    jarWithGroup.id should not be jar.id

    f.deleteOnExit()
  }

  @Test
  def testUploadOutOfLimitFile(): Unit = {
    val bytes = new Array[Byte](RouteUtils.DEFAULT_JAR_SIZE_BYTES.toInt + 1)
    val f = tmpFile(bytes)

    an[IllegalArgumentException] should be thrownBy result(jarApi.upload(f, None))

    f.deleteOnExit()
  }

  @Test
  def testUploadWithNewName(): Unit = {
    val data = methodName().getBytes
    val f = tmpFile(data)
    val jar1 = result(jarApi.upload(f, "xxxx", None))
    jar1.size shouldBe f.length()
    jar1.name shouldBe "xxxx"
    result(jarApi.list).size shouldBe 1

    val jar2 = result(jarApi.upload(f, "yyyy", None))
    jar1.id should not be jar2.id

    val jar3 = result(jarApi.upload(f, "xxxx", Some(GROUP)))
    val jar4 = result(jarApi.upload(f, "yyyy", Some(GROUP)))
    result(jarApi.list).size shouldBe 4
    jar3.group shouldBe GROUP
    jar4.group shouldBe GROUP
    jar3.id should not be jar4.id

    f.deleteOnExit()
  }

  @Test
  def testUploadSameNameFile(): Unit = {
    val data = methodName().getBytes
    val f = tmpFile(data)

    val jar1 = result(jarApi.upload(f, "barfoo.jar", None))
    TimeUnit.SECONDS.sleep(3)
    f.setLastModified(System.currentTimeMillis())
    val jar2 = result(jarApi.upload(f, "barfoo.jar", None))

    val jar3 = result(jarApi.upload(f, "barfoo.jar", Some(GROUP)))
    TimeUnit.SECONDS.sleep(3)
    f.setLastModified(System.currentTimeMillis())
    val jar4 = result(jarApi.upload(f, "barfoo.jar", Some(GROUP)))

    // will get latest modified file of same name files
    // different group means different files (even if same name)
    val res = result(jarApi.list)
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
    val jar = result(jarApi.upload(f, None))
    jar.size shouldBe f.length()
    jar.name shouldBe f.getName
    result(jarApi.list).size shouldBe 1

    result(jarApi.delete(jar.id))
    result(jarApi.list).size shouldBe 0

    f.deleteOnExit()
  }

  @Test
  def testCannotDeleteJarUsedInStreamApp(): Unit = {
    val data = methodName().getBytes
    val f = tmpFile(data)
    // upload jar
    val streamJar = result(accessStreamList.upload(Seq(f.getPath), None)).head
    // create streamApp property
    result(accessStreamProperty.add(StreamPropertyRequest(streamJar.id, None, None, None, None)))
    // cannot delete a used jar
    an[RuntimeException] should be thrownBy result(jarApi.delete(streamJar.id))

    f.deleteOnExit()
  }

  @Test
  def duplicateDeleteStreamProperty(): Unit =
    (0 to 10).foreach(_ => result(jarApi.delete(CommonUtils.randomString(5))))

  @After
  def tearDown(): Unit = Releasable.close(configurator)

}
