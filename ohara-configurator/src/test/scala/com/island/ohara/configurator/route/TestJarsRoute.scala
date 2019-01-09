package com.island.ohara.configurator.route

import java.io.{File, FileOutputStream}

import com.island.ohara.client.configurator.v0.JarApi
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.ReleaseOnce
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
class TestJarsRoute extends SmallTest with Matchers {

  private[this] val configurator = Configurator.fake()
  private[this] val access = JarApi.access().hostname(configurator.hostname).port(configurator.port)

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
    val jar = Await.result(access.upload(f), 30 seconds)
    jar.size shouldBe f.length()
    jar.name shouldBe f.getName
    Await.result(access.list(), 30 seconds).size shouldBe 1
  }

  @Test
  def testUploadWithNewName(): Unit = {
    val data = methodName().getBytes
    val f = tmpFile(data)
    val jar = Await.result(access.upload(f, "xxxx"), 30 seconds)
    jar.size shouldBe f.length()
    jar.name shouldBe "xxxx"
    Await.result(access.list(), 30 seconds).size shouldBe 1
  }

  @Test
  def testDelete(): Unit = {
    val data = methodName().getBytes
    val f = tmpFile(data)
    val jar = Await.result(access.upload(f), 30 seconds)
    jar.size shouldBe f.length()
    jar.name shouldBe f.getName
    Await.result(access.list(), 30 seconds).size shouldBe 1

    Await.result(access.delete(jar.id), 30 seconds) shouldBe jar
    Await.result(access.list(), 30 seconds).size shouldBe 0
  }

  @After
  def tearDown(): Unit = ReleaseOnce.close(configurator)

}
