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

package com.island.ohara.configurator
import java.io.{File, FileOutputStream}
import java.nio.file.Files

import com.island.ohara.client.configurator.v0.JarApi
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
class TestJarStore extends SmallTest with Matchers {

  private[this] val configurator = Configurator.builder().fake().build()

  private[this] val access =
    JarApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] def generateFile(bytes: Array[Byte]): File = {
    val tempFile = CommonUtils.createTempFile(methodName())
    val output = new FileOutputStream(tempFile)
    try output.write(bytes)
    finally output.close()
    tempFile
  }

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def listNonexistentId(): Unit = {
    an[NoSuchElementException] should be thrownBy result(configurator.jarStore.jarInfo("Asdasd"))
    an[NoSuchElementException] should be thrownBy result(configurator.jarStore.jarInfos(Seq("Asdasd")))
    an[NoSuchElementException] should be thrownBy result(configurator.jarStore.url("Asdasd"))
    an[NoSuchElementException] should be thrownBy result(configurator.jarStore.urls(Seq("Asdasd")))
  }

  @Test
  def listNonexistentIdWithExistOne(): Unit = {
    val f = generateFile(CommonUtils.randomString().getBytes)
    val plugin = result(access.upload(f))
    val jars = result(configurator.jarStore.jarInfos(Seq(plugin.id)))
    jars.size shouldBe 1
    jars.head shouldBe plugin

    result(configurator.jarStore.jarInfo(plugin.id)) shouldBe plugin

    an[NoSuchElementException] should be thrownBy result(configurator.jarStore.jarInfos(Seq("Asdasd", plugin.id)))
    an[NoSuchElementException] should be thrownBy result(configurator.jarStore.urls(Seq("Asdasd", plugin.id)))
  }

  @Test
  def testDownload(): Unit = {
    val content = CommonUtils.randomString()
    val f = generateFile(content.getBytes)

    val plugin = result(access.upload(f))
    plugin.name shouldBe f.getName
    plugin.size shouldBe content.length
    result(access.list).size shouldBe 1

    val url = result(configurator.jarStore.url(plugin.id))
    url.getProtocol shouldBe "http"
    val input = url.openStream()
    val tempFile = CommonUtils.createTempFile(methodName())
    if (tempFile.exists()) tempFile.delete() shouldBe true
    try {
      Files.copy(input, tempFile.toPath)
    } finally input.close()
    tempFile.length() shouldBe plugin.size
    new String(Files.readAllBytes(tempFile.toPath)) shouldBe content
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
