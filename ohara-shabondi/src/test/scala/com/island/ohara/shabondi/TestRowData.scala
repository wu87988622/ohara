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

package com.island.ohara.shabondi

import com.island.ohara.common.data.Cell
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.shabondi.JsonSupport._
import org.junit.Test
import org.scalatest.Matchers
import spray.json._

final class TestRowData extends OharaTest with Matchers {
  @Test
  def testRowData(): Unit = {
    val jsonData =
      """
        |{"col1":"hello", "col2": 200}
        |""".stripMargin

    val rowData: RowData = JsonSupport.rowDataFormat.read(jsonData.parseJson)

    rowData("col1") should ===(JsString("hello"))
    rowData("col2") should ===(JsNumber(200))

    val row = JsonSupport.toRow(rowData)

    row.cell(0) should ===(Cell.of("col1", "hello"))
    row.cell(1) should ===(Cell.of("col2", 200))
  }
}
