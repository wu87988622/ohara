package com.island.ohara.configurator.serialization

import com.island.ohara.configurator.data._
import com.island.ohara.rule.SmallTest
import com.island.ohara.serialization.{BYTES, INT}
import org.junit.Test
import org.scalatest.Matchers

class TestOharaDataSerializer extends SmallTest with Matchers {

  @Test
  def testSerialization(): Unit = {
    val dataList: Array[OharaData] = Array(
      OharaTopic("uuid", "name", 100, 9),
      OharaSchema("uuid", "name", Map("a" -> BYTES, "c" -> INT)),
      OharaStreaming("uuid", "name", "c", "d"),
      OharaSource("uuid", "name", Map("a" -> "b", "c" -> "d")),
      OharaTarget("uuid", "name", Map("a" -> "b", "c" -> "d")),
      OharaJob("uuid", "name", JobStatus.RUNNING, Map("a" -> Array("b", "c"), "c" -> Array("d", "aaa")))
    )

    val serializer = OharaDataSerializer
    dataList.foreach(data => {
      val another = serializer.from(serializer.to(data))
      another.equals(data) shouldBe true
    })
  }

}
