package com.island.ohara.configurator
import java.io.{File, FileOutputStream}
import java.nio.file.Files

import com.island.ohara.client.configurator.v0.JarApi
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class TestJarStore extends SmallTest with Matchers {

  private[this] val configurator = Configurator.fake()

  private[this] val access =
    JarApi.access().hostname(configurator.hostname).port(configurator.port)

  private[this] def generateFile(bytes: Array[Byte]): File = {
    val tempFile = CommonUtil.createTempFile(methodName())
    val output = new FileOutputStream(tempFile)
    try output.write(bytes)
    finally output.close()
    tempFile
  }

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def testDownload(): Unit = {
    val content = methodName()
    val f = generateFile(content.getBytes)

    val plugin = result(access.upload(f))
    plugin.name shouldBe f.getName
    plugin.size shouldBe content.length
    result(access.list()).size shouldBe 1

    val url = result(configurator.jarStore.url(plugin.id))
    url.getProtocol shouldBe "http"
    val input = url.openStream()
    val tempFile = CommonUtil.createTempFile(methodName())
    if (tempFile.exists()) tempFile.delete() shouldBe true
    try {
      Files.copy(input, tempFile.toPath)
    } finally input.close()
    tempFile.length() shouldBe plugin.size
    new String(Files.readAllBytes(tempFile.toPath)) shouldBe content
  }

  @After
  def tearDown(): Unit = ReleaseOnce.close(configurator)
}
