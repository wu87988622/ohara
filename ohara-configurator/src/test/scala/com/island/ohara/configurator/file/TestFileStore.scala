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

package com.island.ohara.configurator.file

import java.io.File
import java.nio.file.Files

import com.island.ohara.client.configurator.v0.FileInfoApi
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import com.island.ohara.kafka.connector.json.ObjectKey
import org.junit.{After, Test}
import org.scalatest.Matchers
import spray.json.{JsNumber, JsString}

import scala.concurrent.ExecutionContext.Implicits.global
class TestFileStore extends SmallTest with Matchers {

  private[this] val configurator = Configurator.builder.fake().build()
  private[this] val fileStore = configurator.fileStore

  @Test
  def testAddFile(): Unit = {
    val file = generateJarFile()
    val info = result(fileStore.fileInfoCreator.file(file).create())
    info.group shouldBe FileInfoApi.GROUP_DEFAULT
    info.name shouldBe file.getName
    info.size shouldBe file.length()
    info.tags shouldBe Map.empty
  }

  @Test
  def testAddFileWithGroup(): Unit = {
    val group = CommonUtils.randomString(10)
    val file = generateJarFile()
    val info = result(fileStore.fileInfoCreator.group(group).file(file).create())
    info.group shouldBe group
    info.name shouldBe file.getName
    info.size shouldBe file.length()
    info.tags shouldBe Map.empty
  }

  @Test
  def testAddFileWithName(): Unit = {
    // the file store in configurator accepts only ".jar"
    val name = CommonUtils.randomString(10) + ".jar"
    val file = generateJarFile()
    val info = result(fileStore.fileInfoCreator.name(name).file(file).create())
    info.group shouldBe FileInfoApi.GROUP_DEFAULT
    info.name shouldBe name
    info.size shouldBe file.length()
    info.tags shouldBe Map.empty
  }

  @Test
  def testAddFileWithTags(): Unit = {
    val file = generateJarFile()
    val tags = Map("a" -> JsString("c"), "b" -> JsNumber(123))
    val info = result(fileStore.fileInfoCreator.file(file).tags(tags).create())
    info.group shouldBe FileInfoApi.GROUP_DEFAULT
    info.name shouldBe file.getName
    info.size shouldBe file.length()
    info.tags shouldBe tags
  }

  @Test
  def testDuplicateAdd(): Unit = {
    val file0 = generateJarFile()
    val tags0 = Map("a" -> JsString("c"), "b" -> JsNumber(123))
    val info0 = result(fileStore.fileInfoCreator.file(file0).tags(tags0).create())
    info0.group shouldBe FileInfoApi.GROUP_DEFAULT
    info0.name shouldBe file0.getName
    info0.size shouldBe file0.length()
    info0.tags shouldBe tags0

    val group = CommonUtils.randomString(5)
    val file1 = generateJarFile()
    val info1 = result(fileStore.fileInfoCreator.group(group).file(file1).tags(Map.empty).create())
    info1.group shouldBe group
    info1.name shouldBe file1.getName
    info1.size shouldBe file1.length()
    info1.tags shouldBe Map.empty

    result(fileStore.fileInfos()).size shouldBe 2
  }

  @Test
  def nullFileInAdd(): Unit = an[NullPointerException] should be thrownBy fileStore.fileInfoCreator.file(null)

  @Test
  def nonexistentFileInAdd(): Unit =
    an[IllegalArgumentException] should be thrownBy fileStore.fileInfoCreator.file(new File(CommonUtils.randomString()))

  @Test
  def nullNameInAdd(): Unit =
    an[NullPointerException] should be thrownBy fileStore.fileInfoCreator.name(null)

  @Test
  def emptyNameInAdd(): Unit =
    an[IllegalArgumentException] should be thrownBy fileStore.fileInfoCreator.name("")

  @Test
  def nullGroupInAdd(): Unit =
    an[NullPointerException] should be thrownBy fileStore.fileInfoCreator.group(null)

  @Test
  def emptyGroupInAdd(): Unit =
    an[IllegalArgumentException] should be thrownBy fileStore.fileInfoCreator.group("")

  @Test
  def nullTagsInAdd(): Unit =
    an[NullPointerException] should be thrownBy fileStore.fileInfoCreator.tags(null)

  @Test
  def nonexistentIdInFileInfo(): Unit =
    an[NoSuchElementException] should be thrownBy result(
      fileStore.fileInfo(ObjectKey.of(CommonUtils.randomString(), CommonUtils.randomString())))

  @Test
  def testInvalidInput(): Unit =
    an[IllegalArgumentException] should be thrownBy FileStore.builder
      .homeFolder(CommonUtils.createTempJar("aa").getCanonicalPath)
      .hostname(CommonUtils.anyLocalAddress())
      .port(CommonUtils.availablePort())
      .build()

  @Test
  def testReopen(): Unit = {
    val folder = CommonUtils.createTempFolder(methodName())
    val port = CommonUtils.availablePort()
    val store0 =
      FileStore.builder.homeFolder(folder.getCanonicalPath).hostname(CommonUtils.anyLocalAddress()).port(port).build()
    val jarInfo = try result(store0.fileInfoCreator.file(generateJarFile()).create())
    finally store0.close()

    val store1 =
      FileStore.builder.homeFolder(folder.getCanonicalPath).hostname(CommonUtils.anyLocalAddress()).port(port).build()
    try {
      result(store1.fileInfos()).size shouldBe 1
      result(store1.fileInfos()).head shouldBe jarInfo
    } finally store1.close()
  }

  @Test
  def testDownload(): Unit = {
    val content = CommonUtils.randomString()
    val file = generateJarFile(content.getBytes)

    val fileApi = FileInfoApi.access.hostname(configurator.hostname).port(configurator.port)
    val group = CommonUtils.randomString()
    val fileInfo = result(fileApi.request.group(group).file(file).upload())
    file.getName shouldBe fileInfo.name
    fileInfo.group shouldBe group
    fileInfo.size shouldBe content.length
    fileInfo.url should not be None
    result(fileApi.list()).size shouldBe 1
    result(fileApi.get(fileInfo.key)) shouldBe fileInfo

    val url = fileInfo.url
    url.getProtocol shouldBe "http"
    val input = url.openStream()
    val tempFile = CommonUtils.createTempJar(methodName())
    if (tempFile.exists()) tempFile.delete() shouldBe true
    try Files.copy(input, tempFile.toPath)
    finally input.close()
    tempFile.length() shouldBe fileInfo.size
    new String(Files.readAllBytes(tempFile.toPath)) shouldBe content
  }

  @Test
  def checkUrl(): Unit = {
    val hostname = CommonUtils.randomString(10)
    val port = CommonUtils.availablePort()
    val store = FileStore.builder
      .homeFolder(CommonUtils.createTempFolder(methodName()).getCanonicalPath)
      .hostname(hostname)
      .port(port)
      .build
    try {
      val jarInfo = result(store.fileInfoCreator.file(generateJarFile()).create())
      jarInfo.url.getHost shouldBe hostname
      jarInfo.url.getPort shouldBe port
    } finally store.close()
  }

  @Test
  def testExtensions(): Unit = {
    val extensions = Set(CommonUtils.randomString(3), CommonUtils.randomString(3), CommonUtils.randomString(3))
    val store = FileStore.builder
      .hostname(CommonUtils.hostname())
      .port(22)
      .homeFolder(CommonUtils.createTempFolder(methodName()).getAbsolutePath)
      .acceptedExtensions(extensions)
      .build
    try {
      val fs = extensions.map(extension => generateFile(s".$extension"))
      val infos = fs.map(f => result(store.fileInfoCreator.file(f).create()))
      infos.size shouldBe 3
      // illegal extension will be rejected
      an[IllegalArgumentException] should be thrownBy result(
        store.fileInfoCreator.file(generateFile(CommonUtils.randomString(5))).create())
    } finally store.close()
  }

  @Test
  def testRemove(): Unit = {
    val f = result(fileStore.fileInfoCreator.file(generateJarFile()).create())
    result(fileStore.fileInfos()).size shouldBe 1
    result(fileStore.remove(f.key))
    result(fileStore.fileInfos()).size shouldBe 0
  }

  @Test
  def updateTags(): Unit = {
    val f0 = result(fileStore.fileInfoCreator.file(generateJarFile()).create())
    f0.tags shouldBe Map.empty
    val tags = Map(
      "a" -> JsString("123213")
    )
    val f1 = result(fileStore.updateTags(f0.key, tags))
    f1.tags shouldBe tags

    an[NoSuchElementException] should be thrownBy result(
      fileStore.updateTags(ObjectKey.of(CommonUtils.randomString(5), f0.name), tags))
    an[NoSuchElementException] should be thrownBy result(
      fileStore.updateTags(ObjectKey.of(f0.group, CommonUtils.randomString(5)), tags))
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
