package com.island.ohara.connector.ftp
import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestFtpProperties extends SmallTest with Matchers {

  @Test
  def testFtpSinkProps(): Unit = {
    val props = FtpSinkProps(
      output = "/output",
      user = "user",
      password = "pwd",
      host = "host",
      port = 123,
      encode = Some("UTF-8")
    )
    val copy = FtpSinkProps(props.toMap)
    copy shouldBe props
  }

  @Test
  def testFtpSinkTaskProps(): Unit = {
    val props = FtpSinkTaskProps(
      output = "/output",
      user = "user",
      password = "pwd",
      host = "host",
      port = 123,
      encode = Some("UTF-8")
    )
    val copy = FtpSinkTaskProps(props.toMap)
    copy shouldBe props
  }

  @Test
  def testFtpSourceProps(): Unit = {
    val props = FtpSourceProps(
      input = "/input",
      output = "/output",
      error = "/error/",
      user = "user",
      password = "pwd",
      host = "host",
      port = 123,
      encode = Some("UTF-8")
    )
    val copy = FtpSourceProps(props.toMap)
    copy shouldBe props
  }

  @Test
  def testFtpSourceTaskProps(): Unit = {
    val props = FtpSourceTaskProps(
      hash = 123,
      total = 4,
      input = "/input",
      output = "/output",
      error = "/error/",
      user = "user",
      password = "pwd",
      host = "host",
      port = 123,
      encode = Some("UTF-8")
    )
    val copy = FtpSourceTaskProps(props.toMap)
    copy shouldBe props
  }
}
