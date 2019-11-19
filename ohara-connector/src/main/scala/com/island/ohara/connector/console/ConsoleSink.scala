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

package com.island.ohara.connector.console

import java.util

import com.island.ohara.common.setting.SettingDef
import com.island.ohara.kafka.connector.{ConnectorVersion, RowSinkConnector, RowSinkTask, TaskSetting}

import scala.collection.JavaConverters._

class ConsoleSink extends RowSinkConnector {
  private[this] var config: TaskSetting                    = _
  override protected def _start(config: TaskSetting): Unit = this.config = config

  override protected def _stop(): Unit = {
    // do nothing
  }

  override protected def _taskClass(): Class[_ <: RowSinkTask] = classOf[ConsoleSinkTask]

  override protected def _taskSettings(maxTasks: Int): util.List[TaskSetting] = Seq.fill(maxTasks)(config).asJava

  override protected def _version: ConnectorVersion = ConnectorVersion.DEFAULT

  override protected def _definitions(): util.List[SettingDef] =
    Seq(
      CONSOLE_FREQUENCE_DEFINITION,
      CONSOLE_ROW_DIVIDER_DEFINITION
    ).asJava
}
