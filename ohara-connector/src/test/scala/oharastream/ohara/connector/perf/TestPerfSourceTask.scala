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

package oharastream.ohara.connector.perf

import oharastream.ohara.common.rule.OharaTest
import oharastream.ohara.kafka.connector.json.ConnectorDefUtils
import org.junit.Test
import org.scalatest.matchers.should.Matchers._

class TestPerfSourceTask extends OharaTest {
  @Test
  def byteArrayShouldBeReused(): Unit = {
    val task = new PerfSourceTask
    task.start(
      java.util.Map.of(
        ConnectorDefUtils.CONNECTOR_KEY_DEFINITION.key,
        "{\"group\": \"g\", \"name\": \"n\"}",
        ConnectorDefUtils.TOPIC_KEYS_DEFINITION.key,
        "[{\"group\": \"g\", \"name\": \"n\"}]",
        PERF_FREQUENCY_KEY,
        "10 milliseconds"
      )
    )
    val records_0 = task.poll()
    java.util.concurrent.TimeUnit.SECONDS.sleep(1)
    val records_1 = task.poll()
    records_0.size() should not be 0
    records_0.size() shouldBe records_1.size()
    (0 until records_0.size()).foreach { index =>
      val row_0 = records_0.get(index).key()
      val row_1 = records_1.get(index).key()
      row_0.isInstanceOf[Array[Byte]] shouldBe true
      row_1.isInstanceOf[Array[Byte]] shouldBe true
      row_0.asInstanceOf[Array[Byte]].hashCode() shouldBe row_1.asInstanceOf[Array[Byte]].hashCode()
    }
  }
}
