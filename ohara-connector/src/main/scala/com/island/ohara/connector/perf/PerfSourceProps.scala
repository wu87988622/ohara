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
import scala.concurrent.duration.Duration

case class PerfSourceProps(batch: Int, freq: Duration) {
  def toMap: Map[String, String] = Map(
    PERF_BATCH -> batch.toString,
    PERF_FREQUENCE -> freq.toString
  )
}

object PerfSourceProps {
  def apply(props: Map[String, String]): PerfSourceProps = PerfSourceProps(
    batch = props.getOrElse(PERF_BATCH, DEFAULT_BATCH.toString).toInt,
    freq = Duration(props.getOrElse(PERF_FREQUENCE, DEFAULT_FREQUENCE.toString)),
  )
}
