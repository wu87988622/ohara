package com.island.ohara.configurator.route
import com.island.ohara.client.ConfiguratorJson.FtpInformationRequest
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestFtpInformationRoute extends SmallTest with Matchers {

  @Test
  def testValidateField(): Unit = {
    an[IllegalArgumentException] should be thrownBy FtpInformationRoute.validateField(
      FtpInformationRequest(
        name = "",
        hostname = "hostname",
        port = 1234,
        user = "ab",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInformationRoute.validateField(
      FtpInformationRequest(
        name = "aa",
        hostname = "",
        port = 1234,
        user = "ab",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInformationRoute.validateField(
      FtpInformationRequest(
        name = "aa",
        hostname = "hostname",
        port = -1,
        user = "ab",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInformationRoute.validateField(
      FtpInformationRequest(
        name = "aa",
        hostname = "hostname",
        port = 0,
        user = "ab",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInformationRoute.validateField(
      FtpInformationRequest(
        name = "aaa",
        hostname = "hostname",
        port = 99999,
        user = "ab",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInformationRoute.validateField(
      FtpInformationRequest(
        name = "aaa",
        hostname = "hostname",
        port = 12345,
        user = "",
        password = "aaa"
      ))

    an[IllegalArgumentException] should be thrownBy FtpInformationRoute.validateField(
      FtpInformationRequest(
        name = "aaa",
        hostname = "hostname",
        port = 12345,
        user = "aa",
        password = ""
      ))

    FtpInformationRoute.validateField(
      FtpInformationRequest(
        name = "aaa",
        hostname = "hostname",
        port = 12345,
        user = "aa",
        password = "aaa"
      ))
  }
}
