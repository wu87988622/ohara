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

package com.island.ohara.connector.perf
import com.island.ohara.kafka.connector.TaskSetting

import scala.concurrent.duration._
import scala.compat.java8.OptionConverters._
case class PerfSourceProps(batch: Int, freq: Duration, cellSize: Int) {
  def toMap: Map[String, String] = Map(
    PERF_BATCH_KEY       -> batch.toString,
    PERF_FREQUENCY_KEY   -> freq.toString,
    PERF_CELL_LENGTH_KEY -> cellSize.toString
  )
}

object PerfSourceProps {
  def apply(settings: TaskSetting): PerfSourceProps = PerfSourceProps(
    batch = settings.intOption(PERF_BATCH_KEY).orElse(PERF_BATCH_DEFAULT),
    freq = settings
      .durationOption(PERF_FREQUENCY_KEY)
      .asScala
      .map(_.toMillis milliseconds)
      .getOrElse(PERF_FREQUENCY_DEFAULT),
    cellSize = settings.intOption(PERF_CELL_LENGTH_KEY).orElse(PERF_CELL_LENGTH_DEFAULT)
  )
}
