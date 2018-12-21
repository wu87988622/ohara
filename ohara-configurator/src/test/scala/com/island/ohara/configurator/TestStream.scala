package com.island.ohara.configurator
import java.io.{File, RandomAccessFile}

import akka.http.scaladsl.model.Multipart.FormData.BodyPart.Strict
import akka.http.scaladsl.model._
import akka.util.ByteString
import com.island.ohara.client.ConfiguratorJson.{
  StreamJar,
  StreamListRequest,
  StreamListResponse,
  StreamPropertyRequest,
  StreamPropertyResponse
}
import com.island.ohara.client.{ConfiguratorClient, StreamClient}
import com.island.ohara.common.rule.MediumTest
import org.apache.commons.io.FileUtils
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.io.Source

class TestStream extends MediumTest with Matchers {

  private[this] val configurator =
    Configurator.builder().hostname("localhost").port(0).noCluster.build()

  private[this] val ip = s"${configurator.hostname}:${configurator.port}"
  private[this] val client = ConfiguratorClient(ip)

  private[this] val pipeline_id = "pipeline"

  private[this] val tmpF1 = File.createTempFile("empty_", ".jar")
  private[this] val tmpF2 = File.createTempFile("empty_", ".jar")
  private[this] val tmpF3 = File.createTempFile("empty_", ".jar")

  @Before
  def setup(): Unit = {
    val baseDir: File = new File(StreamClient.JARS_ROOT.toUri)
    if (!baseDir.exists()) baseDir.mkdir()
  }

  @Test
  def testStreamRequest_ListPage(): Unit = {
    // Test POST method
    val multipartForm =
      Multipart.FormData(
        Multipart.FormData.BodyPart.Strict(
          "streamapp",
          HttpEntity(ContentType(MediaTypes.`application/java-archive`), ByteString(Source.fromFile(tmpF1).mkString)),
          Map("filename" -> tmpF1.getName)
        ),
        Multipart.FormData.BodyPart.Strict(
          "streamapp",
          HttpEntity(ContentType(MediaTypes.`application/java-archive`), ByteString(Source.fromFile(tmpF2).mkString)),
          Map("filename" -> tmpF2.getName)
        )
      )
    val res1 = client.streamUploadJars[Strict, StreamListResponse](pipeline_id, multipartForm.strictParts: _*)
    res1.jars.foreach(jar => {
      jar.jarName.startsWith("empty_") shouldBe true
    })

    // Test GET method
    val res2 = client.list[StreamJar](pipeline_id)
    res2.size shouldBe 2

    // Test DELETE method
    val deleteJar = res1.jars.head
    val d = client.delete[StreamJar](deleteJar.id)
    d.jarName shouldBe deleteJar.jarName

    //Test PUT method
    val originJar = res1.jars.last
    val anotherJar = StreamListRequest("la-new.jar")
    val updated = client.update[StreamListRequest, StreamJar](originJar.id, anotherJar)
    updated.jarName shouldBe "la-new.jar"
  }

  @Test
  def testAbnormalStreamRequest_ListPage(): Unit = {
    // Test POST method
    // Testing wrong input key
    val multipartForm =
      Multipart.FormData(
        Multipart.FormData.BodyPart.Strict(
          "fake_key",
          HttpEntity(ContentType(MediaTypes.`application/java-archive`), ByteString(Source.fromFile(tmpF3).mkString)),
          Map("filename" -> tmpF3.getName)
        )
      )
    val res = client.streamUploadJars[Strict, StreamListResponse](pipeline_id, multipartForm.strictParts: _*)
    res.jars.length shouldBe 0

    // Testing exceed maximum file size
    val raf: RandomAccessFile = new RandomAccessFile(tmpF3, "rw")
    raf.setLength(StreamClient.MAX_FILE_SIZE + 1)
    val bigMultipartForm =
      Multipart.FormData(
        Multipart.FormData.BodyPart.Strict(
          "streamapp",
          HttpEntity(ContentType(MediaTypes.`application/java-archive`), ByteString(Source.fromFile(tmpF3).mkString)),
          Map("filename" -> tmpF3.getName)
        )
      )
    an[IllegalArgumentException] should be thrownBy client
      .streamUploadJars[Strict, StreamListResponse](pipeline_id, bigMultipartForm.strictParts: _*)

    // Test DELETE method
    // no such id
    an[IllegalArgumentException] should be thrownBy client.delete[StreamJar]("fake-id")

    //Test PUT method
    // no such id
    val anotherJar = StreamListRequest("la-new.jar")
    an[IllegalArgumentException] should be thrownBy client.update[StreamListRequest, StreamJar]("fake-id", anotherJar)

    // request require jarName
    val otherJar = StreamListRequest("")
    an[IllegalArgumentException] should be thrownBy client.update[StreamListRequest, StreamJar]("fake-id", otherJar)
  }

  @Test
  def testStreamRequest_PropertyPage(): Unit = {
    val multipartForm =
      Multipart.FormData(
        Multipart.FormData.BodyPart.Strict(
          "streamapp",
          HttpEntity(ContentType(MediaTypes.`application/java-archive`), ByteString(Source.fromFile(tmpF1).mkString)),
          Map("filename" -> tmpF1.getName)
        )
      )
    val jarData =
      client.streamUploadJars[Strict, StreamListResponse](pipeline_id, multipartForm.strictParts: _*)

    // Test GET method
    val id = jarData.jars.head.id
    val res1 = client.get[StreamPropertyResponse](id)
    res1.id shouldBe id
    res1.fromTopics.size shouldBe 0
    res1.toTopics.size shouldBe 0
    res1.instances shouldBe 1

    // Test PUT method
    val req = StreamPropertyRequest("my-app", Seq("from-topic"), Seq("to-topic"), 2)
    val res2 = client.update[StreamPropertyRequest, StreamPropertyResponse](id, req)
    res2.name shouldBe "my-app"
    res2.fromTopics.size shouldBe 1
    res2.toTopics.size shouldBe 1
    res2.instances shouldBe 2
  }

  @Test
  def testAbnormalStreamRequest_PropertyPage(): Unit = {
    // Test GET method
    // no such id
    an[IllegalArgumentException] should be thrownBy client.get[StreamPropertyResponse]("fake-id")

    // Test PUT method
    // instances must bigger than 1
    val req1 = StreamPropertyRequest("my-app", List.empty, Seq("to-topic"), -1)
    an[IllegalArgumentException] should be thrownBy client
      .update[StreamPropertyRequest, StreamPropertyResponse]("fake-id", req1)

    // no such id
    val req2 = StreamPropertyRequest("my-app", Seq.empty, Seq("to-topic"), 3)
    an[IllegalArgumentException] should be thrownBy client
      .update[StreamPropertyRequest, StreamPropertyResponse]("fake-id", req2)
  }

  @After
  def tearDown(): Unit = {
    tmpF1.deleteOnExit()
    tmpF2.deleteOnExit()
    tmpF3.deleteOnExit()

    val f: File = StreamClient.JARS_ROOT.toFile
    FileUtils.cleanDirectory(f)
  }
}
