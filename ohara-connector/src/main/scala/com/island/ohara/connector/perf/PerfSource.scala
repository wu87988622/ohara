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
import com.island.ohara.kafka.connector.json.SettingDefinition
import com.island.ohara.kafka.connector.{ConnectorVersion, RowSourceConnector, RowSourceTask, TaskConfig}

import scala.collection.JavaConverters._

class PerfSource extends RowSourceConnector {
  private[this] var config: TaskConfig = _

  override protected def _taskClass(): Class[_ <: RowSourceTask] = classOf[PerfSourceTask]

  override protected def _taskConfigs(maxTasks: Int): java.util.List[TaskConfig] = Seq.fill(maxTasks)(config).asJava

  /**
    * this method is exposed to test scope
    */
  @VisibleForTesting
  override protected[perf] def _start(config: TaskConfig): Unit = {
    if (config.topicNames().isEmpty) throw new IllegalArgumentException("topics can't be empty")
    val props = PerfSourceProps(config.raw().asScala.toMap)
    if (props.batch < 0) throw new IllegalArgumentException(s"batch:${props.batch} can't be negative")
    this.config = config
  }

  override protected def _stop(): Unit = {}

  override protected def _definitions(): java.util.List[SettingDefinition] = Seq(
    SettingDefinition
      .builder()
      .displayName("Batch")
      .documentation("The batch of perf")
      .valueType(SettingDefinition.Type.INT)
      .key(PERF_BATCH)
      .optional("10")
      .build(),
    SettingDefinition
      .builder()
      .displayName("Frequence")
      .documentation("The frequence of perf")
      .valueType(SettingDefinition.Type.STRING)
      .key(PERF_FREQUENCE)
      .optional("1 second")
      .build(),
  ).asJava

  override protected def _version: ConnectorVersion = ConnectorVersion.DEFAULT
}
