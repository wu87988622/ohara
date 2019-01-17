/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.client
import com.island.ohara.common.data.{Column, DataType}
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._
class TestConfiguratorJson extends SmallTest with Matchers {

  @Test
  def testEmptySchema(): Unit = {
    val schema = Column.toColumns(Column.fromColumns(Seq.empty.asJava))
    schema.size shouldBe 0
  }

  @Test
  def testColumnString(): Unit = {
    val schema =
      Seq(Column.of("a", DataType.STRING, 0),
          Column.of("c", "d", DataType.INT, 1),
          Column.of("e", "f", DataType.DOUBLE, 2))
    schema shouldBe Column.toColumns(Column.fromColumns(schema.asJava)).asScala
  }
}
