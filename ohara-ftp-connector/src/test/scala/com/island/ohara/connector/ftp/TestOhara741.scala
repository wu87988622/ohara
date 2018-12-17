package com.island.ohara.connector.ftp
import com.island.ohara.client.FtpClient
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.ReleaseOnce
import com.island.ohara.integration.FtpServer
import com.island.ohara.kafka.connector.TaskConfig
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestOhara741 extends SmallTest with Matchers {

  private[this] val ftpServer = FtpServer.local(0, Array(0))

  @Test
  def testAutoCreateOutput(): Unit = {
    val props = FtpSinkProps(
      output = "/output",
      needHeader = false,
      user = ftpServer.user,
      password = ftpServer.password,
      hostname = ftpServer.hostname,
      port = ftpServer.port,
      encode = Some("UTF-8")
    )

    val taskConfig = new TaskConfig(
      "aa",
      Seq.empty.asJava,
      Seq.empty.asJava,
      props.toMap.asJava
    )

    val sink = new FtpSink
    sink._start(taskConfig)

    val ftpClient = FtpClient
      .builder()
      .hostname(ftpServer.hostname)
      .port(ftpServer.port)
      .user(ftpServer.user)
      .password(ftpServer.password)
      .build()
    try ftpClient.exist("/output") shouldBe true
    finally ftpClient.close()
  }

  @After
  def tearDown(): Unit = {
    ReleaseOnce.close(ftpServer)
  }
}
