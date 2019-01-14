package com.island.ohara.connector.ftp

import com.island.ohara.client.FtpClient
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.ReleaseOnce
import com.island.ohara.integration.FtpServer
import com.island.ohara.kafka.connector.TaskConfig
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestOhara743 extends SmallTest with Matchers {

  private[this] val ftpServer = FtpServer.local(0, Array(0))

  @Test
  def testAutoCreateOutput(): Unit = {
    val props = FtpSourceProps(
      inputFolder = "/input",
      completedFolder = "/output",
      errorFolder = "/error",
      user = ftpServer.user,
      password = ftpServer.password,
      hostname = ftpServer.hostname,
      port = ftpServer.port,
      encode = Some("UTF-8")
    )

    val taskConfig = TaskConfig.builder().name("aa").options(props.toMap.asJava).build()

    val ftpClient = FtpClient
      .builder()
      .hostname(ftpServer.hostname)
      .port(ftpServer.port)
      .user(ftpServer.user)
      .password(ftpServer.password)
      .build()

    try {
      ftpClient.mkdir(props.inputFolder)
      val source = new FtpSource
      source._start(taskConfig)
      ftpClient.exist(props.errorFolder) shouldBe true
      ftpClient.exist(props.completedFolder.get) shouldBe true
    } finally ftpClient.close()
  }

  @After
  def tearDown(): Unit = {
    ReleaseOnce.close(ftpServer)
  }
}
