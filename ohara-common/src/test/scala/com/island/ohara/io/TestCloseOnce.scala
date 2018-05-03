package com.island.ohara.io

import java.io.IOException

import com.island.ohara.io.CloseOnce._
import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestCloseOnce extends SmallTest with Matchers {

  @Test
  def testFinalClose(): Unit = {
    def invalidString(): CloseOnce = throw new IOException("IOE")

    try {
      doClose(invalidString)(_ => true)
      fail("It should fail")
    } catch {
      case _: IOException => {}
    }

    def validString(): CloseOnce = () => {}

    doClose(validString)(_ => true) shouldBe true
    try {
      doFinally(invalidString)(_ => true)(_ => {})
      fail("It should fail")
    } catch {
      case _: IOException => {}
    }
    doFinally(validString)(_ => true)(_ => {}) shouldBe true
  }
}
