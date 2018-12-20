package com.island.ohara.configurator
import java.io.File
import java.nio.file.Paths

import akka.http.scaladsl.model.Multipart.FormData.BodyPart.Strict
import akka.http.scaladsl.model._
import akka.util.ByteString
import com.island.ohara.client.ConfiguratorClient
import com.island.ohara.client.ConfiguratorJson.{StreamJarUpdateRequest, StreamObj, StreamResponse}
import com.island.ohara.common.rule.MediumTest
import org.apache.commons.io.FileUtils
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.io.Source

class TestStream extends MediumTest with Matchers {

  private[this] val configurator =
    Configurator.builder().hostname("localhost").port(0).noCluster.build()

  private[this] val ip = s"${configurator.hostname}:${configurator.port}"
  private[this] val client = ConfiguratorClient(ip)

  private[this] val tmpF1 = File.createTempFile("empty_", ".jar")
  private[this] val tmpF2 = File.createTempFile("empty_", ".jar")

  private[this] val TMP_ROOT = System.getProperty("java.io.tmpdir")
  private[this] val JARS_ROOT = Paths.get(TMP_ROOT, "ohara_streams")

  @Test
  def testStreamRequest_jars(): Unit = {
    // Test POST method
    val multipartForm =
      Multipart.FormData(
        Multipart.FormData.BodyPart.Strict(
          "java",
          HttpEntity(ContentType(MediaTypes.`application/java-archive`), ByteString(Source.fromFile(tmpF1).mkString)),
          Map("filename" -> tmpF1.getName)
        ),
        Multipart.FormData.BodyPart.Strict(
          "java",
          HttpEntity(ContentType(MediaTypes.`application/java-archive`), ByteString(Source.fromFile(tmpF2).mkString)),
          Map("filename" -> tmpF2.getName)
        )
      )
    val res = client.stream_jars_uploadJar[Strict, StreamResponse](multipartForm.strictParts: _*)
    res.jars.foreach(jar => {
      jar.jarName.startsWith("empty_") shouldBe true
    })

    // Test GET method
    val jars = client.list[StreamObj]
    jars.size shouldBe 2

    // Test DELETE method
    val deleteJar = jars.head
    val d = client.delete[StreamObj](deleteJar.id)
    d.jarName shouldBe deleteJar.jarName

    //Test PUT method
    val originJar = jars.last
    val anotherJar = StreamJarUpdateRequest("la-new.jar")
    val updated = client.update[StreamJarUpdateRequest, StreamObj](originJar.id, anotherJar)
    updated.jarName shouldBe "la-new.jar"
  }

  @After
  def tearDown(): Unit = {
    tmpF1.deleteOnExit()
    tmpF2.deleteOnExit()

    val f: File = JARS_ROOT.toFile
    FileUtils.cleanDirectory(f)
  }
}
