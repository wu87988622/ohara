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

import com.island.ohara.client.configurator.v0.JarApi
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
class TestJarsRoute extends SmallTest with Matchers {

  private[this] val configurator = Configurator.builder().fake().build()
  private[this] val jarApi = JarApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] def tmpFile(bytes: Array[Byte]): File = {
    val f = File.createTempFile(methodName(), null)
    val output = new FileOutputStream(f)
    try output.write(bytes)
    finally output.close()
    f
  }
  @Test
  def testUpload(): Unit = {
    val data = methodName().getBytes
    val f = tmpFile(data)
    val jar = Await.result(jarApi.upload(f), 30 seconds)
    jar.size shouldBe f.length()
    jar.name shouldBe f.getName
    Await.result(jarApi.list, 30 seconds).size shouldBe 1
  }

  @Test
  def testUploadWithNewName(): Unit = {
    val data = methodName().getBytes
    val f = tmpFile(data)
    val jar = Await.result(jarApi.upload(f, "xxxx"), 30 seconds)
    jar.size shouldBe f.length()
    jar.name shouldBe "xxxx"
    Await.result(jarApi.list, 30 seconds).size shouldBe 1
  }

  @Test
  def testDelete(): Unit = {
    val data = methodName().getBytes
    val f = tmpFile(data)
    val jar = Await.result(jarApi.upload(f), 30 seconds)
    jar.size shouldBe f.length()
    jar.name shouldBe f.getName
    Await.result(jarApi.list, 30 seconds).size shouldBe 1

    Await.result(jarApi.delete(jar.id), 30 seconds)
    Await.result(jarApi.list, 30 seconds).size shouldBe 0
  }

  @Test
  def duplicateDeleteStreamProperty(): Unit =
    (0 to 10).foreach(_ => result(jarApi.delete(CommonUtils.randomString(5))))

  @After
  def tearDown(): Unit = Releasable.close(configurator)

}
