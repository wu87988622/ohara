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

package com.island.ohara.connector.hdfs.sink

import java.util

import com.island.ohara.common.setting.SettingDef
import com.island.ohara.kafka.connector._
import com.island.ohara.kafka.connector.csv.CsvSinkConnector

import scala.collection.JavaConverters._

/**
  * This class extends RowSinkConnector abstract.
  */
class HDFSSink extends CsvSinkConnector {
  private[this] var settings: TaskSetting = _

  override protected[hdfs] def _start(settings: TaskSetting): Unit = {
    this.settings = settings
  }

  override protected def _stop(): Unit = {
    //TODO
  }

  override protected def _taskClass(): Class[_ <: RowSinkTask] = classOf[HDFSSinkTask]

  override protected[hdfs] def _taskSettings(maxTasks: Int): util.List[TaskSetting] = {
    Seq.fill(maxTasks) { settings }.asJava
  }

  override protected def customCsvSettingDefinitions(): util.Map[String, SettingDef] = DEFINITIONS.asJava
}
