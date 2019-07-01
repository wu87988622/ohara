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

package com.island.ohara.configurator.jar

import java.io.{File, FileOutputStream}
import java.nio.file.Files
import java.util.concurrent.TimeUnit

import com.island.ohara.client.configurator.v0.JarApi
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestJarStore extends SmallTest with Matchers {

  private[this] val configurator = Configurator.builder().fake().build()

  private[this] val access =
    JarApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] def generateFile(bytes: Array[Byte]): File = {
    val tempFile = CommonUtils.createTempJar(methodName())
    val output = new FileOutputStream(tempFile)
    try output.write(bytes)
    finally output.close()
    tempFile.deleteOnExit()
    tempFile
  }
  private[this] def generateFile(): File = generateFile(CommonUtils.randomString().getBytes)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def testGroupLabelWithConfigurator(): Unit = {
    val group = "foobar"

    // add a random group folder file
    result(configurator.jarStore.add(generateFile()))
    // add two files in specific group
    val f1 = generateFile()
    result(configurator.jarStore.add(f1, "newfile", group))
    // make sure the second file has different modified time
    TimeUnit.SECONDS.sleep(2)
    val f2 = generateFile()
    val info2 = result(configurator.jarStore.add(f2, "newfile", group))

    // use same group and same file, but request different newName means different file
    result(configurator.jarStore.add(f2, "farboo", group))

    val jars = result(configurator.jarStore.jarInfos())
    // we only get the "latest modified" file in same group, so we have
    // 1. file with random group
    // 2. "newfile" file with specific group (upload two files but get last modified time)
    // 3. "farboo" file with specific group
    jars.size shouldBe 3

    // filter jars by group
    result(configurator.jarStore.jarInfos(group)).size shouldBe 2

    // we only can get the latest modified file of same group and same name
    val res = result(configurator.jarStore.jarInfo(info2.group, info2.name))
    res.group shouldBe group
    res.size shouldBe f2.length()
    res.name shouldBe "newfile"
  }

  @Test
  def nullGroupJarInfo(): Unit =
    an[NullPointerException] should be thrownBy result(configurator.jarStore.jarInfo(null, "name"))

  @Test
  def emptyGroupInJarInfo(): Unit =
    an[IllegalArgumentException] should be thrownBy result(configurator.jarStore.jarInfo("", "name"))

  @Test
  def nullIdInJarInfo(): Unit =
    an[NullPointerException] should be thrownBy result(configurator.jarStore.jarInfo("group", null))

  @Test
  def emptyIdInJarInfo(): Unit =
    an[IllegalArgumentException] should be thrownBy result(configurator.jarStore.jarInfo("group", ""))

  @Test
  def nullFileInAdd(): Unit = an[NullPointerException] should be thrownBy result(configurator.jarStore.add(null))

  @Test
  def nonexistentFileInAdd(): Unit =
    an[IllegalArgumentException] should be thrownBy result(
      configurator.jarStore.add(new File(CommonUtils.randomString()), ""))

  @Test
  def nullFileInAdd2(): Unit =
    an[NullPointerException] should be thrownBy result(configurator.jarStore.add(null, "aa"))

  @Test
  def nonexistentFileInAdd2(): Unit = an[IllegalArgumentException] should be thrownBy result(
    configurator.jarStore.add(new File(CommonUtils.randomString()), "aa"))

  @Test
  def nullNewNameInAdd(): Unit =
    an[NullPointerException] should be thrownBy result(configurator.jarStore.add(generateFile(), null))

  @Test
  def emptyNameInAdd(): Unit =
    an[IllegalArgumentException] should be thrownBy result(configurator.jarStore.add(generateFile(), ""))

  @Test
  def nullIdInUpdate(): Unit =
    an[NullPointerException] should be thrownBy result(configurator.jarStore.update("group", null, generateFile()))

  @Test
  def emptyIdInUpdate(): Unit =
    an[IllegalArgumentException] should be thrownBy result(configurator.jarStore.update("group", "", generateFile()))

  @Test
  def nullFileInUpdate(): Unit = {
    val jarInfo = result(configurator.jarStore.add(generateFile()))
    an[NullPointerException] should be thrownBy result(configurator.jarStore.update(jarInfo.group, jarInfo.name, null))
  }

  @Test
  def nonexistentFileInUpdate(): Unit = {
    val jarInfo = result(configurator.jarStore.add(generateFile()))
    an[IllegalArgumentException] should be thrownBy result(
      configurator.jarStore.update(jarInfo.group, jarInfo.name, new File(CommonUtils.randomString())))
  }

  @Test
  def nonexistentIdInJarInfo(): Unit =
    an[NoSuchElementException] should be thrownBy result(configurator.jarStore.jarInfo("group", "Asdasd"))

  @Test
  def nullIdInRename(): Unit =
    an[NullPointerException] should be thrownBy result(
      configurator.jarStore.rename("group", null, CommonUtils.randomString()))

  @Test
  def emptyIdInRename(): Unit =
    an[IllegalArgumentException] should be thrownBy result(
      configurator.jarStore.rename("group", "", CommonUtils.randomString()))

  @Test
  def nullNewNameInRename(): Unit = {
    val jarInfo = result(configurator.jarStore.add(generateFile()))
    an[NullPointerException] should be thrownBy result(configurator.jarStore.rename(jarInfo.group, jarInfo.name, null))
  }

  @Test
  def emptyNewNameInRename(): Unit = {
    val jarInfo = result(configurator.jarStore.add(generateFile()))
    an[IllegalArgumentException] should be thrownBy result(
      configurator.jarStore.rename(jarInfo.group, jarInfo.name, ""))
  }

  @Test
  def nullIdInToFile(): Unit =
    an[NullPointerException] should be thrownBy result(configurator.jarStore.toFile("group", null))

  @Test
  def emptyIdInToFile(): Unit =
    an[IllegalArgumentException] should be thrownBy result(configurator.jarStore.toFile("group", ""))

  @Test
  def listNonexistentIdWithExistOne(): Unit = {
    val f = generateFile(CommonUtils.randomString().getBytes)
    val plugin = result(access.request.upload(f))
    val jar = result(configurator.jarStore.jarInfo(plugin.group, plugin.name))
    // the jar info from jar store does not have the url
    jar.copy(url = plugin.url) shouldBe plugin
  }

  @Test
  def testInvalidInput(): Unit =
    an[IllegalArgumentException] should be thrownBy JarStore.builder
      .homeFolder(CommonUtils.createTempJar("aa").getCanonicalPath)
      .hostname(CommonUtils.anyLocalAddress())
      .port(CommonUtils.availablePort())
      .build()

  @Test
  def testReopen(): Unit = {
    val folder = CommonUtils.createTempFolder(methodName())
    val content = CommonUtils.randomString()
    val port = CommonUtils.availablePort()
    val store0 =
      JarStore.builder.homeFolder(folder.getCanonicalPath).hostname(CommonUtils.anyLocalAddress()).port(port).build()
    val jarInfo = try result(store0.add(generateFile(content.getBytes)))
    finally store0.close()

    val store1 =
      JarStore.builder.homeFolder(folder.getCanonicalPath).hostname(CommonUtils.anyLocalAddress()).port(port).build()
    try {
      result(store1.jarInfos()).size shouldBe 1
      result(store1.jarInfos()).head shouldBe jarInfo
    } finally store1.close()
  }

  @Test
  def testDownload(): Unit = {
    val content = CommonUtils.randomString()
    val f = generateFile(content.getBytes)

    val plugin = result(access.request.upload(f))
    f.getName.contains(plugin.name) shouldBe true
    plugin.size shouldBe content.length
    plugin.url should not be None
    result(access.request.list()).size shouldBe 1
    result(access.request.group(plugin.group).get(plugin.name)) shouldBe plugin

    val url = plugin.url
    url.getProtocol shouldBe "http"
    val input = url.openStream()
    val tempFile = CommonUtils.createTempJar(methodName())
    if (tempFile.exists()) tempFile.delete() shouldBe true
    try {
      Files.copy(input, tempFile.toPath)
    } finally input.close()
    tempFile.length() shouldBe plugin.size
    new String(Files.readAllBytes(tempFile.toPath)) shouldBe content
  }

  @Test
  def checkUrl(): Unit = {
    val hostname = CommonUtils.randomString(10)
    val port = CommonUtils.availablePort()
    val store = JarStore.builder
      .homeFolder(CommonUtils.createTempFolder(methodName()).getCanonicalPath)
      .hostname(hostname)
      .port(port)
      .build()
    try {
      val jarInfo = result(store.add(generateFile(methodName().getBytes)))
      jarInfo.url.getHost shouldBe hostname
      jarInfo.url.getPort shouldBe port
    } finally store.close()
  }

  @Test
  def nullHomeFolder(): Unit = an[NullPointerException] should be thrownBy JarStore.builder.homeFolder(null)

  @Test
  def emptyHomeFolder(): Unit = an[IllegalArgumentException] should be thrownBy JarStore.builder.homeFolder("")

  @Test
  def nullHostname(): Unit = an[NullPointerException] should be thrownBy JarStore.builder.hostname(null)

  @Test
  def emptyHostname(): Unit = an[IllegalArgumentException] should be thrownBy JarStore.builder.hostname("")

  @Test
  def navigatePort(): Unit = an[IllegalArgumentException] should be thrownBy JarStore.builder.port(-1)

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
