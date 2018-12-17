package com.island.ohara.it

import java.nio.charset.Charset

import com.island.ohara.client.{FileType, FtpClient}
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.{ByteUtil, ReleaseOnce}
import com.island.ohara.integration.FtpServer
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

class TestFtpClient extends MediumTest with Matchers {

  private[this] val server = FtpServer.of()

  private[this] val client =
    FtpClient.builder().user(server.user).password(server.password).hostname(server.hostname).port(server.port).build()

  private[this] def tmpPath() = s"${client.tmpFolder}/$methodName"

  @Before
  def setup(): Unit = if (!client.exist(client.tmpFolder)) client.mkdir(client.tmpFolder)

  @Test
  def testDelete(): Unit = {
    val before = client.listFileNames(client.tmpFolder).size
    client.attach(tmpPath(), "hello world")
    val after = client.listFileNames(client.tmpFolder).size
    after - before shouldBe 1

    client.delete(tmpPath())
    client.listFileNames(client.tmpFolder).size shouldBe before
  }

  @Test
  def testList(): Unit = {
    if (client.exist(tmpPath())) client.delete(tmpPath())
    val before = client.listFileNames(client.tmpFolder).size
    client.attach(tmpPath(), "message")
    val after = client.listFileNames(client.tmpFolder).size
    after - before shouldBe 1
  }

  @Test
  def testReadWrite(): Unit = {
    val content = "abcdefg--------1235"
    val bytes = content.getBytes(Charset.forName("UTF-8"))
    val output = client.create(tmpPath())
    try output.write(bytes)
    finally output.close()

    val input = client.open(tmpPath())
    try {
      val buf = new Array[Byte](bytes.length)
      var offset = 0
      while (offset != buf.length) {
        val rval = input.read(buf, offset, buf.length - offset)
        if (rval == -1) throw new IllegalArgumentException(s"Failed to read data from ${tmpPath()}")
        offset += rval
      }
      val copy = new String(buf, Charset.forName("UTF-8"))
      copy shouldBe content
    } finally input.close()
  }

  @Test
  def testReadLine(): Unit = {
    if (client.exist(tmpPath())) client.delete(tmpPath())
    val lineCount = 100
    client.attach(tmpPath(), (0 until lineCount).map(_.toString))
    client.readLines(tmpPath()).length shouldBe lineCount
  }

  @Test
  def testMove(): Unit = {
    val lineCount = 100
    client.attach(tmpPath(), (0 until lineCount).map(_.toString))

    val folder = s"${client.tmpFolder}/hello"
    if (client.exist(folder)) {
      client.delete(folder)
    }
    client.mkdir(folder)
    client.listFileNames(folder).size shouldBe 0
    client.moveFile(tmpPath(), s"$folder/$methodName")
    client.listFileNames(folder).size shouldBe 1
  }

  @Test
  def testUpload(): Unit = {
    val data = tmpPath()
    val bytes = data.getBytes(Charset.forName("UTF-8"))
    if (client.exist(tmpPath())) client.delete(tmpPath())
    client.upload(tmpPath(), bytes)
    data shouldBe new String(client.download(tmpPath()), Charset.forName("UTF-8"))
  }

  @Test
  def testFileType(): Unit = {
    val path = methodName
    if (client.exist(path)) client.delete(path)
    client.fileType(path) shouldBe FileType.NONEXISTENT

    client.attach(path, "abc")
    withClue(s"client:${client.getClass.getName}")(client.fileType(path) shouldBe FileType.FILE)

    client.delete(path)
    client.fileType(path) shouldBe FileType.NONEXISTENT

    client.mkdir(path)
    client.fileType(path) shouldBe FileType.FOLDER
  }

  @Test
  def testDeleteFolder(): Unit = {
    val data = ByteUtil.toBytes(methodName)
    val folder = s"/$methodName"
    client.mkdir(folder)
    client.upload(s"$folder/file", data)
    client.listFileNames(folder).size shouldBe 1

    an[IllegalArgumentException] should be thrownBy client.delete(folder)
    client.delete(s"$folder/file")
    client.delete(folder)
    client.listFileNames(folder).size shouldBe 0
  }

  @Test
  def testAppend(): Unit = {
    val path = methodName
    client.fileType(path) shouldBe FileType.NONEXISTENT

    client.attach(path, "abc")
    client.attach(path, "ccc")
    val results = client.readLines(path)
    results.length shouldBe 2
    results(0) shouldBe "abc"
    results(1) shouldBe "ccc"
  }

  @Test
  def testCreateAndAppend(): Unit = {
    val path = methodName

    an[IllegalArgumentException] should be thrownBy client.append(path)

    client.attach(path, "abc")
    an[IllegalArgumentException] should be thrownBy client.create(path)
    client.append(path).close()
    client.delete(path)
  }

  @After
  def tearDown(): Unit = {
    ReleaseOnce.close(client)
    ReleaseOnce.close(server)
  }
}
