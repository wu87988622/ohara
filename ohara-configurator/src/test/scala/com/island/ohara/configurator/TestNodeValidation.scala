package com.island.ohara.configurator
import com.island.ohara.client.configurator.v0.ValidationApi
import com.island.ohara.client.configurator.v0.ValidationApi.NodeValidationRequest
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.ReleaseOnce
import com.island.ohara.integration.SshdServer
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._

class TestNodeValidation extends SmallTest with Matchers {

  private[this] val configurator = Configurator.local()
  private[this] val sshd = SshdServer.local()

  @Test
  def test(): Unit = {
    val reports = Await.result(
      ValidationApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .verify(
          NodeValidationRequest(
            hostname = sshd.hostname(),
            port = sshd.port(),
            user = sshd.user(),
            password = sshd.password()
          )),
      10 seconds
    )
    reports.isEmpty shouldBe false
    reports.foreach(report => withClue(report.message)(report.pass shouldBe true))
  }

  @After
  def tearDown(): Unit = {
    ReleaseOnce.close(configurator)
    ReleaseOnce.close(sshd)
  }
}
