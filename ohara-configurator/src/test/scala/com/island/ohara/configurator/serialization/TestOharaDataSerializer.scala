package com.island.ohara.configurator.serialization

import com.island.ohara.data._
import com.island.ohara.rule.SmallTest
import com.island.ohara.serialization.{BYTES, INT, OharaDataSerializer}
import org.junit.Test
import org.scalatest.Matchers

class TestOharaDataSerializer extends SmallTest with Matchers {

  @Test
  def testSerialization(): Unit = {
    val dataList: Array[OharaData] = Array(
      OharaTopic("uuid", "name", 100, 9),
      OharaSchema("uuid", "name", Map("a" -> BYTES, "c" -> INT), Map("a" -> 0, "c" -> 1), false),
      OharaStreaming("uuid", "name", "c", "d"),
      OharaSource("uuid", "name", Map("a" -> "b", "c" -> "d")),
      OharaTarget("uuid", "name", Map("a" -> "b", "c" -> "d")),
      OharaPipeline("uuid", "name", PipelineStatus.RUNNING, Map("a" -> Array("b", "c"), "c" -> Array("d", "aaa")))
    )

    val serializer = OharaDataSerializer
    dataList.foreach(data => {
      val another = serializer.from(serializer.to(data))
      another.equals(data) shouldBe true
    })
  }

}
