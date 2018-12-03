package com.island.ohara.integration

import com.island.ohara.common.rule.MediumTest
import org.junit.Test
import org.scalatest.Matchers

class TestOharaTestUtil extends MediumTest with Matchers {

  private[this] def setEnv(key: String, value: String) = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    val map = field.get(System.getenv()).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
    map.put(key, value)
  }

  @Test
  def testLocalMethod(): Unit = {
    setEnv("ohara.it.workers", "123")
    val util = OharaTestUtil.workers()
    try an[RuntimeException] should be thrownBy util.brokersConnProps finally util.close()
  }
}
