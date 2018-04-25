package com.island.ohara.io

import java.io.IOException

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import com.island.ohara.io.CloseOnce._

@RunWith(classOf[JUnitRunner])
class TestCloseOnce extends FlatSpec with Matchers {

  "The close method" should "be invoked" in {
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
