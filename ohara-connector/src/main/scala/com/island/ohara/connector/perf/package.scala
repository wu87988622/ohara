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

package com.island.ohara.connector

import com.island.ohara.common.data.{Column, DataType}

import scala.concurrent.duration.Duration

package object perf {
  val PERF_BATCH: String = "perf.batch"
  val PERF_FREQUENCE: String = "perf.frequence"

  val DEFAULT_BATCH: Int = 10
  val DEFAULT_FREQUENCE: Duration = Duration("1 second")

  /**
    * this is the default schema used to generate random data in perf source.
    * Since schema is not "required" in ohara, making a default schema avoid confusing users when they miss the schema
    */
  val DEFAULT_SCHEMA: Seq[Column] = Seq(
    Column.builder().name("a").dataType(DataType.STRING).order(0).build(),
    Column.builder().name("b").dataType(DataType.STRING).order(1).build(),
    Column.builder().name("c").dataType(DataType.STRING).order(2).build()
  )
}
