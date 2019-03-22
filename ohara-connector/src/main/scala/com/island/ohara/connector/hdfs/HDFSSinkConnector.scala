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

package com.island.ohara.connector.hdfs
import java.util

import com.island.ohara.common.util.VersionUtils
import com.island.ohara.kafka.connector.{RowSinkConnector, RowSinkTask, TaskConfig}

import scala.collection.JavaConverters._

/**
  * This class extends RowSinkConnector abstract.
  */
class HDFSSinkConnector extends RowSinkConnector {

  var props: TaskConfig = _

  override protected[hdfs] def _start(config: TaskConfig): Unit = {
    this.props = config
  }

  override protected def _stop(): Unit = {
    //TODO
  }

  override protected def _taskClass(): Class[_ <: RowSinkTask] = classOf[HDFSSinkTask]

  override protected[hdfs] def _taskConfigs(maxTasks: Int): util.List[TaskConfig] = {
    Seq.fill(maxTasks) { props }.asJava
  }

  override protected def _version: String = VersionUtils.VERSION

  override protected def revision: String = VersionUtils.REVISION

  override protected def author: String = "ohara"
}
