package com.island.ohara.client
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._
class TestFtpClientBuilder extends SmallTest with Matchers {

  @Test
  def testArguments(): Unit = {
    // pass
    FtpClient
      .builder()
      .hostname("abc")
      .port(123)
      .user("adasd")
      .password("asda")
      .retryCount(10)
      .retryInternal(5 seconds)
      .build()
      .close()

    an[IllegalArgumentException] should be thrownBy FtpClient
      .builder()
      .port(123)
      .user("adasd")
      .password("asda")
      .retryCount(10)
      .retryInternal(5 seconds)
      .build()

    an[IllegalArgumentException] should be thrownBy FtpClient
      .builder()
      .hostname("abc")
      .user("adasd")
      .password("asda")
      .retryCount(10)
      .retryInternal(5 seconds)
      .build()

    an[IllegalArgumentException] should be thrownBy FtpClient
      .builder()
      .hostname("abc")
      .port(123)
      .password("asda")
      .retryCount(10)
      .retryInternal(5 seconds)
      .build()

    an[IllegalArgumentException] should be thrownBy FtpClient
      .builder()
      .hostname("abc")
      .port(123)
      .user("adasd")
      .retryCount(10)
      .retryInternal(5 seconds)
      .build()

    an[IllegalArgumentException] should be thrownBy FtpClient
      .builder()
      .hostname("abc")
      .port(123)
      .user("adasd")
      .password("asda")
      .retryCount(10)
      .retryInternal(-5 seconds)
      .build()

    an[IllegalArgumentException] should be thrownBy FtpClient
      .builder()
      .hostname("abc")
      .port(-123)
      .user("adasd")
      .password("asda")
      .retryCount(10)
      .retryInternal(5 seconds)
      .build()
  }
}
