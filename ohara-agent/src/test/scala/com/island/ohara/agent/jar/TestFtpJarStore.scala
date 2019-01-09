package com.island.ohara.agent.jar
import java.io.{File, FileOutputStream}
import java.nio.file.Files

import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce}
import org.junit.{After, Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestFtpJarStore extends MediumTest with Matchers {
  private[this] val numberOfFtpThreads = 10
  private[this] val tmpFolder = CommonUtil.createTempDir(classOf[TestFtpJarStore].getSimpleName)
  private[this] val jarStore = JarStore.ftp(tmpFolder.getAbsolutePath, numberOfFtpThreads)

  private[this] def generateFile(bytes: Array[Byte]): File = {
    val tempFile = CommonUtil.createTempFile(methodName())
    val output = new FileOutputStream(tempFile)
    try output.write(bytes)
    finally output.close()
    tempFile
  }

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)
  @Before
  def setup(): Unit = {
    result(jarStore.jarInfos()).size shouldBe 0
  }

  @Test
  def testInvalidHomeFolder(): Unit = {
    an[IllegalArgumentException] should be thrownBy new FtpJarStore("/home/", 0, Array(0))
    an[IllegalArgumentException] should be thrownBy new FtpJarStore("home", 0, Array(0))
  }

  private[this] def assert(id: String): Unit = {
    val files = tmpFolder.listFiles()
    (files == null) shouldBe false
    files.count(_.isDirectory) shouldBe 1

    val files2 = new File(tmpFolder, id).listFiles()
    (files2 == null) shouldBe false
    files2.count(_.isFile) shouldBe 1
  }
  @Test
  def testAdd(): Unit = {
    val content = methodName()
    val f = generateFile(content.getBytes)
    val plugin = Await.result(jarStore.add(f), 30 seconds)
    plugin.size shouldBe content.length
    result(jarStore.jarInfos()).size shouldBe 1
    assert(plugin.id)
  }

  @Test
  def testInvalidId(): Unit = {
    an[IllegalArgumentException] should be thrownBy result(jarStore.jarInfo(null))
    an[IllegalArgumentException] should be thrownBy result(jarStore.jarInfo(""))
    an[IllegalArgumentException] should be thrownBy result(jarStore.remove(null))
    an[IllegalArgumentException] should be thrownBy result(jarStore.remove(""))
    an[IllegalArgumentException] should be thrownBy result(jarStore.url(null))
    an[IllegalArgumentException] should be thrownBy result(jarStore.url(""))
    an[IllegalArgumentException] should be thrownBy result(jarStore.update("", null))
    an[IllegalArgumentException] should be thrownBy result(jarStore.update("", null))
  }

  @Test
  def testDownload(): Unit = {
    val content = methodName()
    val f = generateFile(content.getBytes)
    val plugin = Await.result(jarStore.add(f), 30 seconds)
    plugin.name shouldBe f.getName
    plugin.size shouldBe content.length
    result(jarStore.jarInfos()).size shouldBe 1

    val url = result(jarStore.url(plugin.id))
    url.getProtocol shouldBe "ftp"
    val input = url.openStream()
    val tempFile = CommonUtil.createTempFile(methodName())
    if (tempFile.exists()) tempFile.delete() shouldBe true
    try {
      Files.copy(input, tempFile.toPath)
    } finally input.close()
    tempFile.length() shouldBe plugin.size
    new String(Files.readAllBytes(tempFile.toPath)) shouldBe content
  }

  @Test
  def testRemove(): Unit = {
    val content = methodName()
    val f = generateFile(content.getBytes)
    val plugin = Await.result(jarStore.add(f), 30 seconds)
    plugin.name shouldBe f.getName
    plugin.size shouldBe content.length
    result(jarStore.jarInfos()).size shouldBe 1
    result(jarStore.remove(plugin.id)) shouldBe plugin
    result(jarStore.jarInfos()).size shouldBe 0

    val files = tmpFolder.listFiles()
    if (files != null) files.count(_.isDirectory) shouldBe 0

    an[NoSuchElementException] should be thrownBy result(jarStore.remove(plugin.id))
  }

  @Test
  def testUpdate(): Unit = {
    val content = methodName()
    val f = generateFile(content.getBytes)
    val plugin = Await.result(jarStore.add(f), 30 seconds)
    plugin.name shouldBe f.getName
    plugin.size shouldBe content.length
    result(jarStore.jarInfos()).size shouldBe 1

    val content2 = methodName() + "-newone"
    val f2 = generateFile(content2.getBytes)
    val plugin2 = Await.result(jarStore.update(plugin.id, f2), 30 seconds)
    plugin2.name shouldBe f2.getName
    plugin2.size shouldBe content2.length
    plugin2.id shouldBe plugin.id
    result(jarStore.jarInfos()).size shouldBe 1
    assert(plugin.id)
  }

  @Test
  def testUpdateNullFile(): Unit = {
    val content = methodName()
    val f = generateFile(content.getBytes)
    val plugin = Await.result(jarStore.add(f), 30 seconds)
    plugin.name shouldBe f.getName
    plugin.size shouldBe content.length
    result(jarStore.jarInfos()).size shouldBe 1

    an[IllegalArgumentException] should be thrownBy result(jarStore.update(plugin.id, null))

    // failed update should not change the state of store
    result(jarStore.jarInfos()).size shouldBe 1
  }

  @Test
  def testMultiAdd(): Unit = {
    (0 until 10).foreach { index =>
      val content = methodName()
      val f = generateFile(content.getBytes)
      val plugin = Await.result(jarStore.add(f), 30 seconds)
      plugin.size shouldBe content.length
      result(jarStore.jarInfos()).size shouldBe (index + 1)
    }
  }

  @Test
  def testMultiDownload(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val content = methodName()
    val f = generateFile(content.getBytes)
    val plugin = Await.result(jarStore.add(f), 30 seconds)
    plugin.size shouldBe content.length
    (0 until numberOfFtpThreads / 2)
      .map { _ =>
        Future {
          val url = result(jarStore.url(plugin.id))
          val input = url.openStream()
          val tempFile = CommonUtil.createTempFile(methodName())
          if (tempFile.exists()) tempFile.delete() shouldBe true
          try {
            Files.copy(input, tempFile.toPath)
          } finally input.close()
          tempFile
        }
      }
      .map(Await.result(_, 30 seconds))
      .foreach(_.length() shouldBe plugin.size)
  }

  @After
  def tearDown(): Unit = {
    ReleaseOnce.close(jarStore)
    CommonUtil.deleteFiles(tmpFolder)
  }
}
