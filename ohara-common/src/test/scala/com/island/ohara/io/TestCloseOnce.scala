package com.island.ohara.io

import java.io.IOException

import com.island.ohara.io.CloseOnce._
import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestCloseOnce extends SmallTest with Matchers {

  @Test
  def testCloseOnlyOnce(): Unit = {
    class SimpleCloser extends CloseOnce {
      var count = 0
      override protected def doClose(): Unit = count += 1
    }
    val simpleCloser = new SimpleCloser
    0 until 10 foreach (_ => simpleCloser.close())
    simpleCloser.count shouldBe 1
  }

  @Test
  def testFinalClose(): Unit = {
    def invalidString: CloseOnce = throw new IOException("IOE")

    try {
      doClose(invalidString)(_ => true)
      fail("It should fail")
    } catch {
      case _: IOException =>
    }

    def validString: CloseOnce = new CloseOnce {
      override protected def doClose(): Unit = {
        // do nothing
      }
    }

    doClose(validString)(_ => true) shouldBe true
    try {
      doFinally(invalidString)(_ => true)(_ => {})
      fail("It should fail")
    } catch {
      case _: IOException =>
    }
    doFinally(validString)(_ => true)(_ => {}) shouldBe true
  }

  @Test
  def testRelease(): Unit = {
    var hasRun = false
    release(() => hasRun = true)
    hasRun shouldBe true
  }
}
