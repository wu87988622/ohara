package com.island.ohara.client.util

import java.io.IOException

import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers
import com.island.ohara.client.util.CloseOnce._

import scala.collection.mutable

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

  @Test
  def testClose2(): Unit = {
    val sets = new mutable.ListBuffer[String]
    val a = new CloseOnce {
      override protected def doClose(): Unit = sets += "a"
    }
    val b = new CloseOnce {
      override protected def doClose(): Unit = sets += "b"
    }
    CloseOnce.doClose2(a)(_ => b)((_, _) => sets += "z")
    sets.size shouldBe 3
    sets(0) shouldBe "z"
    sets(1) shouldBe "b"
    sets(2) shouldBe "a"
  }

  @Test
  def close2ShouldReleaseAllResources(): Unit = {
    val sets = new mutable.ListBuffer[String]
    val a = new CloseOnce {
      override protected def doClose(): Unit = sets += "a"
    }
    an[IllegalArgumentException] should be thrownBy CloseOnce.doClose2(a)(_ => throw new IllegalArgumentException)(
      (_, _) => sets += "z")
    sets.size shouldBe 1
    sets(0) shouldBe "a"
  }

  @Test
  def testClose3(): Unit = {
    val sets = new mutable.ListBuffer[String]
    val a = new CloseOnce {
      override protected def doClose(): Unit = sets += "a"
    }
    val b = new CloseOnce {
      override protected def doClose(): Unit = sets += "b"
    }
    val c = new CloseOnce {
      override protected def doClose(): Unit = sets += "c"
    }
    CloseOnce.doClose3(a)(_ => b)(_ => c)((_, _, _) => sets += "z")
    sets.size shouldBe 4
    sets(0) shouldBe "z"
    sets(1) shouldBe "c"
    sets(2) shouldBe "b"
    sets(3) shouldBe "a"
  }

  @Test
  def close3ShouldReleaseAllResources(): Unit = {
    val sets = new mutable.ListBuffer[String]
    val a = new CloseOnce {
      override protected def doClose(): Unit = sets += "a"
    }
    val b = new CloseOnce {
      override protected def doClose(): Unit = sets += "b"
    }
    an[IllegalArgumentException] should be thrownBy CloseOnce.doClose3(a)(_ => b)(_ =>
      throw new IllegalArgumentException)((_, _, _) => sets += "z")
    sets.size shouldBe 2
    sets(0) shouldBe "b"
    sets(1) shouldBe "a"
  }

  @Test
  def testClose4(): Unit = {
    val sets = new mutable.ListBuffer[String]
    val a = new CloseOnce {
      override protected def doClose(): Unit = sets += "a"
    }
    val b = new CloseOnce {
      override protected def doClose(): Unit = sets += "b"
    }
    val c = new CloseOnce {
      override protected def doClose(): Unit = sets += "c"
    }
    val d = new CloseOnce {
      override protected def doClose(): Unit = sets += "d"
    }
    CloseOnce.doClose4(a)(_ => b)(_ => c)(_ => d)((_, _, _, _) => sets += "z")
    sets.size shouldBe 5
    sets(0) shouldBe "z"
    sets(1) shouldBe "d"
    sets(2) shouldBe "c"
    sets(3) shouldBe "b"
    sets(4) shouldBe "a"
  }

  @Test
  def close4ShouldReleaseAllResources(): Unit = {
    val sets = new mutable.ListBuffer[String]
    val a = new CloseOnce {
      override protected def doClose(): Unit = sets += "a"
    }
    val b = new CloseOnce {
      override protected def doClose(): Unit = sets += "b"
    }
    val c = new CloseOnce {
      override protected def doClose(): Unit = sets += "c"
    }
    an[IllegalArgumentException] should be thrownBy CloseOnce.doClose4(a)(_ => b)(_ => c)(_ =>
      throw new IllegalArgumentException)((_, _, _, _) => sets += "z")
    sets.size shouldBe 3
    sets(0) shouldBe "c"
    sets(1) shouldBe "b"
    sets(2) shouldBe "a"
  }

  @Test
  def testClose5(): Unit = {
    val sets = new mutable.ListBuffer[String]
    val a = new CloseOnce {
      override protected def doClose(): Unit = sets += "a"
    }
    val b = new CloseOnce {
      override protected def doClose(): Unit = sets += "b"
    }
    val c = new CloseOnce {
      override protected def doClose(): Unit = sets += "c"
    }
    val d = new CloseOnce {
      override protected def doClose(): Unit = sets += "d"
    }
    val e = new CloseOnce {
      override protected def doClose(): Unit = sets += "e"
    }
    CloseOnce.doClose5(a)(_ => b)(_ => c)(_ => d)(_ => e)((_, _, _, _, _) => sets += "z")
    sets.size shouldBe 6
    sets(0) shouldBe "z"
    sets(1) shouldBe "e"
    sets(2) shouldBe "d"
    sets(3) shouldBe "c"
    sets(4) shouldBe "b"
    sets(5) shouldBe "a"
  }

  @Test
  def close5ShouldReleaseAllResources(): Unit = {
    val sets = new mutable.ListBuffer[String]
    val a = new CloseOnce {
      override protected def doClose(): Unit = sets += "a"
    }
    val b = new CloseOnce {
      override protected def doClose(): Unit = sets += "b"
    }
    val c = new CloseOnce {
      override protected def doClose(): Unit = sets += "c"
    }
    val d = new CloseOnce {
      override protected def doClose(): Unit = sets += "d"
    }
    an[IllegalArgumentException] should be thrownBy CloseOnce.doClose5(a)(_ => b)(_ => c)(_ => d)(_ =>
      throw new IllegalArgumentException)((_, _, _, _, _) => sets += "z")
    sets.size shouldBe 4
    sets(0) shouldBe "d"
    sets(1) shouldBe "c"
    sets(2) shouldBe "b"
    sets(3) shouldBe "a"
  }
}
