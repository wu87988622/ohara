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

package com.island.ohara.configurator

import com.island.ohara.kafka.connector.{RowSinkConnector, RowSinkTask, TaskSetting}

import scala.collection.JavaConverters._
class DumbSink extends RowSinkConnector {
  private[this] var settings: TaskSetting                                          = _
  override protected def _taskClass(): Class[_ <: RowSinkTask]                     = classOf[DumbSinkTask]
  override protected def _taskSettings(maxTasks: Int): java.util.List[TaskSetting] = Seq.fill(maxTasks)(settings).asJava
  override protected def _start(settings: TaskSetting): Unit = {
    this.settings = settings
    if (settings.stringOption("you_should_fail").isPresent) throw new IllegalArgumentException
  }
  override protected def _stop(): Unit = {}
}
