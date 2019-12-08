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

import com.island.ohara.common.data.{Cell, Column, DataType, Row}
import com.island.ohara.common.util.{ByteUtils, CommonUtils}

import scala.concurrent.duration.Duration

package object perf {
  val PERF_BATCH: String     = "perf.batch"
  val PERF_FREQUENCE: String = "perf.frequence"

  val DEFAULT_BATCH: Int          = 10
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

  def toJavaDuration(d: Duration): java.time.Duration = java.time.Duration.ofMillis(d.toMillis)
  def toScalaDuration(d: java.time.Duration): Duration =
    Duration(d.toMillis, java.util.concurrent.TimeUnit.MILLISECONDS)

  /**
    * generate a row according to input schema
    * @param schema schema
    * @return row
    */
  def row(schema: Seq[Column]): Row = Row.of(
    schema.sortBy(_.order).map { c =>
      Cell.of(
        c.newName,
        c.dataType match {
          case DataType.BOOLEAN => false
          case DataType.BYTE    => ByteUtils.toBytes(CommonUtils.current()).head
          case DataType.BYTES   => ByteUtils.toBytes(CommonUtils.current())
          case DataType.SHORT   => CommonUtils.current().toShort
          case DataType.INT     => CommonUtils.current().toInt
          case DataType.LONG    => CommonUtils.current()
          case DataType.FLOAT   => CommonUtils.current().toFloat
          case DataType.DOUBLE  => CommonUtils.current().toDouble
          case DataType.STRING  => CommonUtils.current().toString
          case _                => CommonUtils.current()
        }
      )
    }: _*
  )
}
