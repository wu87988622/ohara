package com.island.ohara.util

import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestSystemUtil extends SmallTest with Matchers {
  @Test
  def testCurrentTimeMillis1(): Unit = {
    SystemUtil.inject(new TestTime(1538969685059L))
    val currentTime: Long = SystemUtil.current()
    currentTime shouldBe 1538969685059L
  }

  @Test
  def testCurrentTimeMillis2(): Unit = {
    val current: Long = SystemUtil.current()
    SystemUtil.inject(new TestTime(current))
    Thread.sleep(1000)
    SystemUtil.current() shouldBe current
  }

  private[this] class TestTime(currentTime: Long) extends Timer {
    override def current(): Long = {
      currentTime
    }
  }
}
