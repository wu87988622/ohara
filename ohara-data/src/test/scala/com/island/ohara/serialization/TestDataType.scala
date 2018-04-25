package com.island.ohara.serialization

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class TestDataType extends FlatSpec with Matchers {

  "The index of all DataTypes " should "different" in {
    collection.SortedSet(DataType.all.map(_.index): _*).size shouldBe DataType.all.size
  }
}
