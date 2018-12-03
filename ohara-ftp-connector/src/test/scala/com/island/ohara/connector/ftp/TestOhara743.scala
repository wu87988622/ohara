package com.island.ohara.connector.ftp

import com.island.ohara.client.FtpClient
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CloseOnce
import com.island.ohara.integration.FtpServer
import com.island.ohara.kafka.connector.TaskConfig
import org.junit.{After, Test}
import org.scalatest.Matchers

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
      host = ftpServer.host,
      port = ftpServer.port,
      encode = Some("UTF-8")
    )

    val taskConfig = TaskConfig(
      name = "aa",
      topics = Seq.empty,
      schema = Seq.empty,
      options = props.toMap
    )

    val ftpClient = FtpClient
      .builder()
      .hostname(ftpServer.host)
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
    CloseOnce.close(ftpServer)
  }
}
