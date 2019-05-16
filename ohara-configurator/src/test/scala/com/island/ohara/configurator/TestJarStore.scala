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
import com.island.ohara.configurator.jar.LocalJarStore
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
  private[this] def generateFile(): File = generateFile(CommonUtils.randomString().getBytes)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def nullIdInJarInfo(): Unit = an[NullPointerException] should be thrownBy result(configurator.jarStore.jarInfo(null))

  @Test
  def emptyIdInJarInfo(): Unit =
    an[IllegalArgumentException] should be thrownBy result(configurator.jarStore.jarInfo(""))

  @Test
  def nullFileInAdd(): Unit = an[NullPointerException] should be thrownBy result(configurator.jarStore.add(null))

  @Test
  def nonexistentFileInAdd(): Unit =
    an[IllegalArgumentException] should be thrownBy result(
      configurator.jarStore.add(new File(CommonUtils.randomString())))

  @Test
  def nullFileInAdd2(): Unit = an[NullPointerException] should be thrownBy result(configurator.jarStore.add(null, "aa"))

  @Test
  def nonexistentFileInAdd2(): Unit = an[IllegalArgumentException] should be thrownBy result(
    configurator.jarStore.add(new File(CommonUtils.randomString(), "aa")))

  @Test
  def nullNewNameInAdd(): Unit =
    an[NullPointerException] should be thrownBy result(configurator.jarStore.add(generateFile(), null))

  @Test
  def emptyNameInAdd(): Unit =
    an[IllegalArgumentException] should be thrownBy result(configurator.jarStore.add(generateFile(), ""))

  @Test
  def nullIdInUpdate(): Unit =
    an[NullPointerException] should be thrownBy result(configurator.jarStore.update(null, generateFile()))

  @Test
  def emptyIdInUpdate(): Unit =
    an[IllegalArgumentException] should be thrownBy result(configurator.jarStore.update("", generateFile()))

  @Test
  def nullFileInUpdate(): Unit = {
    val jarInfo = result(configurator.jarStore.add(generateFile()))
    an[NullPointerException] should be thrownBy result(configurator.jarStore.update(jarInfo.id, null))
  }

  @Test
  def nonexistentFileInUpdate(): Unit = {
    val jarInfo = result(configurator.jarStore.add(generateFile()))
    an[IllegalArgumentException] should be thrownBy result(
      configurator.jarStore.update(jarInfo.id, new File(CommonUtils.randomString())))
  }

  @Test
  def nonexistentIdInJarInfo(): Unit =
    an[NoSuchElementException] should be thrownBy result(configurator.jarStore.jarInfo("Asdasd"))

  @Test
  def nullIdInRename(): Unit =
    an[NullPointerException] should be thrownBy result(configurator.jarStore.rename(null, CommonUtils.randomString()))

  @Test
  def emptyIdInRename(): Unit =
    an[IllegalArgumentException] should be thrownBy result(configurator.jarStore.rename("", CommonUtils.randomString()))

  @Test
  def nullNewNameInRename(): Unit = {
    val jarInfo = result(configurator.jarStore.add(generateFile()))
    an[NullPointerException] should be thrownBy result(configurator.jarStore.rename(jarInfo.id, null))
  }

  @Test
  def emptyNewNameInRename(): Unit = {
    val jarInfo = result(configurator.jarStore.add(generateFile()))
    an[IllegalArgumentException] should be thrownBy result(configurator.jarStore.rename(jarInfo.id, ""))
  }

  @Test
  def nullIdInToFile(): Unit = an[NullPointerException] should be thrownBy result(configurator.jarStore.toFile(null))

  @Test
  def emptyIdInToFile(): Unit = an[IllegalArgumentException] should be thrownBy result(configurator.jarStore.toFile(""))

  @Test
  def listNonexistentIdWithExistOne(): Unit = {
    val f = generateFile(CommonUtils.randomString().getBytes)
    val plugin = result(access.upload(f))
    val jar = result(configurator.jarStore.jarInfo(plugin.id))
    jar shouldBe plugin

    result(configurator.jarStore.jarInfo(plugin.id)) shouldBe plugin
  }

  @Test
  def testInvalidInput(): Unit =
    an[IllegalArgumentException] should be thrownBy new LocalJarStore(CommonUtils.createTempFile("aa").getCanonicalPath)

  @Test
  def testReopen(): Unit = {
    val folder = CommonUtils.createTempFolder(methodName())
    val content = CommonUtils.randomString()
    val store0 = new LocalJarStore(folder.getCanonicalPath)
    val jarInfo = try result(store0.add(generateFile(content.getBytes)))
    finally store0.close()

    val store1 = new LocalJarStore(folder.getCanonicalPath)
    try {
      result(store1.jarInfos).size shouldBe 1
      result(store1.jarInfos).head shouldBe jarInfo
    } finally store1.close()
  }

  @Test
  def testDownload(): Unit = {
    val content = CommonUtils.randomString()
    val f = generateFile(content.getBytes)

    val plugin = result(access.upload(f))
    plugin.name shouldBe f.getName
    plugin.size shouldBe content.length
    result(access.list).size shouldBe 1

    val url = result(configurator.urlGenerator.url(plugin.id))
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
