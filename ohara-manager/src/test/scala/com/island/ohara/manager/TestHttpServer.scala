package com.island.ohara.manager

import java.io.File

import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestHttpServer extends SmallTest with Matchers {

  @Test
  def testWebRoot(): Unit = {
    val tmpFile = new File("xxxx")
    if (tmpFile.exists()) tmpFile.delete()
    try {
      the[RuntimeException] thrownBy HttpServer.webRoot(tmpFile.getAbsolutePath)
      tmpFile.mkdir() shouldBe true
      HttpServer.webRoot(tmpFile.getAbsolutePath)
      tmpFile.delete() shouldBe true
      the[RuntimeException] thrownBy HttpServer.webRoot(tmpFile.getAbsolutePath)
      System.setProperty(HttpServer.PROP_WEBROOT, System.getProperty("java.io.tmpdir"))
      HttpServer.webRoot(tmpFile.getAbsolutePath)
    } finally if (tmpFile.exists()) tmpFile.delete()
  }
}
