package com.island.ohara.client
import java.nio.charset.Charset

import com.island.ohara.io.CloseOnce._
import com.island.ohara.integration.FtpServer
import com.island.ohara.io.CloseOnce
import com.island.ohara.rule.MediumTest
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

class TestFtpClient extends MediumTest with Matchers {

  private[this] val server = FtpServer()

  private[this] val client = FtpClient.builder
    .host(server.host)
    .port(server.port)
    .user(server.writableUser.name)
    .password(server.writableUser.password)
    .build()

  private[this] def tmpPath(client: FtpClient) = s"${client.tmpFolder}/$methodName"

  @Before
  def setup(): Unit = {
    if (!client.exist(client.tmpFolder)) client.mkdir(client.tmpFolder)
  }
  @Test
  def testDelete(): Unit = {
    val before = client.listFileNames(client.tmpFolder).size
    client.append(tmpPath(client), "hello world")
    val after = client.listFileNames(client.tmpFolder).size
    after - before shouldBe 1

    client.delete(tmpPath(client))
    client.listFileNames(client.tmpFolder).size shouldBe before
  }

  @Test
  def testList(): Unit = {
    val path = tmpPath(client)
    if (client.exist(path)) client.delete(path)
    val before = client.listFileNames(client.tmpFolder).size
    client.append(tmpPath(client), "message")
    val after = client.listFileNames(client.tmpFolder).size
    after - before shouldBe 1
  }

  @Test
  def testReadWrite(): Unit = {
    val content = "abcdefg--------1235"
    val bytes = content.getBytes(Charset.forName("UTF-8"))
    doClose(client.create(tmpPath(client))) { output =>
      output.write(bytes)
    }

    doClose(client.open(tmpPath(client))) { input =>
      {
        val buf = new Array[Byte](bytes.length)
        var offset = 0
        while (offset != buf.length) {
          val rval = input.read(buf, offset, buf.length - offset)
          if (rval == -1) throw new IllegalArgumentException(s"Failed to read data from ${tmpPath(client)}")
          offset += rval
        }
        val copy = new String(buf, Charset.forName("UTF-8"))
        copy shouldBe content
      }
    }
  }

  @Test
  def testReadLine(): Unit = {
    if (client.exist(tmpPath(client))) client.delete(tmpPath(client))
    val lineCount = 100
    client.append(tmpPath(client), (0 until lineCount).map(_.toString))
    client.readLines(tmpPath(client)).size shouldBe lineCount

    client.readLines(tmpPath(client), s => s.toInt < 50).size shouldBe 50
    client.readLines(tmpPath(client), s => s.toInt > lineCount).size shouldBe 0
  }

  @Test
  def testMove(): Unit = {
    val lineCount = 100
    client.append(tmpPath(client), (0 until lineCount).map(_.toString))

    val folder = s"${client.tmpFolder}/hello world"
    if (client.exist(folder)) client.delete(folder)
    client.mkdir(folder)
    client.listFileNames(folder).size shouldBe 0
    client.moveFile(tmpPath(client), s"$folder/$methodName")
    client.listFileNames(folder).size shouldBe 1
  }

  @Test
  def testUpload(): Unit = {
    val data = tmpPath(client)
    val bytes = data.getBytes(Charset.forName("UTF-8"))
    if (client.exist(tmpPath(client))) client.delete(tmpPath(client))
    client.upload(tmpPath(client), bytes)
    data shouldBe new String(client.download(tmpPath(client)), Charset.forName("UTF-8"))
  }

  @Test
  def testFileType(): Unit = {
    val path = methodName
    client.fileType(path) shouldBe FileType.NONEXISTENT

    client.append(path, "abc")
    withClue(s"client:${client.getClass.getName}")(client.fileType(path) shouldBe FileType.FILE)

    client.delete(path)
    client.fileType(path) shouldBe FileType.NONEXISTENT

    client.mkdir(path)
    client.fileType(path) shouldBe FileType.FOLDER
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(client)
    CloseOnce.close(server)
  }
}
