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
import com.island.ohara.common.annotations.VisibleForTesting
import com.island.ohara.common.setting.SettingDef
import com.island.ohara.kafka.connector.{ConnectorVersion, RowSourceConnector, RowSourceTask, TaskSetting}

import scala.collection.JavaConverters._

class PerfSource extends RowSourceConnector {
  @VisibleForTesting
  private[perf] var settings: TaskSetting = _

  override protected def _taskClass(): Class[_ <: RowSourceTask] = classOf[PerfSourceTask]

  override protected def _taskSettings(maxTasks: Int): java.util.List[TaskSetting] = Seq.fill(maxTasks)(settings).asJava

  /**
    * this method is exposed to test scope
    */
  override protected def _start(settings: TaskSetting): Unit = {
    if (settings.topicNames().isEmpty) throw new IllegalArgumentException("topics can't be empty")
    val props = PerfSourceProps(settings)
    if (props.batch < 0) throw new IllegalArgumentException(s"batch:${props.batch} can't be negative")
    this.settings = settings
  }

  override protected def _stop(): Unit = {}

  override protected def _definitions(): java.util.List[SettingDef] = Seq(
    SettingDef
      .builder()
      .displayName("Batch")
      .documentation("The batch of perf")
      .valueType(SettingDef.Type.INT)
      .key(PERF_BATCH)
      .optional(DEFAULT_BATCH.toString)
      .build(),
    SettingDef
      .builder()
      .displayName("Frequence")
      .documentation("The frequence of perf")
      .valueType(SettingDef.Type.DURATION)
      .key(PERF_FREQUENCE)
      .optional(toJavaDuration(DEFAULT_FREQUENCE).toString)
      .build(),
  ).asJava

  override protected def _version: ConnectorVersion = ConnectorVersion.DEFAULT
}
