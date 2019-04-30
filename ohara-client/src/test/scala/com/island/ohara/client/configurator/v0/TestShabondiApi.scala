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

package com.island.ohara.client.configurator.v0

import com.island.ohara.client.configurator.v0.ShabondiApi._
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestShabondiApi extends SmallTest with Matchers {

  @Test
  def testJsonConversion(): Unit = {
    import spray.json._

    val desc = ShabondiDescription("id1", "name1", 123, Some("RUNNING"), Seq.empty, 129, 1)
    val json1 = SHABONDI_DESCRIPTION_JSON_FORMAT.write(desc).toString()
    json1 should be(
      """{"name":"name1","state":"RUNNING","lastModified":123,"instances":1,"to":[],"id":"id1","port":129}""")

    val desc2 = ShabondiDescription("id1", "name1", 123, None, Seq("topic1"), 129, 1)
    val json2 = SHABONDI_DESCRIPTION_JSON_FORMAT.write(desc2).toString()
    json2 should be("""{"name":"name1","lastModified":123,"instances":1,"to":["topic1"],"id":"id1","port":129}""")

    val jsonValue =
      """
        |{
        |  "id":"eeee-ffff-gggg-hhhh",
        |  "name": "name2",
        |  "state": "RUNNING",
        |  "to": ["topic1"],
        |  "lastModified": 12345,
        |  "instances": 3,
        |  "port":3200
        |}
      """.stripMargin.parseJson
    val desc3: ShabondiDescription = SHABONDI_DESCRIPTION_JSON_FORMAT.read(jsonValue)

    desc3.id should be("eeee-ffff-gggg-hhhh")
    desc3.name should be("name2")
    desc3.state should be(Some("RUNNING"))
    desc3.to should be(Seq("topic1"))
    desc3.lastModified should be(12345)
    desc3.instances should be(3)
    desc3.port should be(3200)
  }

}
