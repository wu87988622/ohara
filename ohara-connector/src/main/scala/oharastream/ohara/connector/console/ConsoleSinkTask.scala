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

package oharastream.ohara.connector.console

import java.util

import oharastream.ohara.common.annotations.VisibleForTesting
import oharastream.ohara.common.util.CommonUtils
import oharastream.ohara.kafka.connector.{RowSinkRecord, RowSinkTask, TaskSetting}
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration._
import scala.collection.JavaConverters._

class ConsoleSinkTask extends RowSinkTask {
  private[this] val LOG = Logger(classOf[ConsoleSinkTask])
  @VisibleForTesting
  private[console] var freq: Duration = CONSOLE_FREQUENCE_DEFAULT
  @VisibleForTesting
  private[console] var divider: String = CONSOLE_ROW_DIVIDER_DEFAULT
  @VisibleForTesting
  private[console] var lastLog: Long = -1
  override protected def run(config: TaskSetting): Unit = {
    divider = config.stringOption(CONSOLE_ROW_DIVIDER).orElse(CONSOLE_ROW_DIVIDER_DEFAULT)
    freq = config
      .durationOption(CONSOLE_FREQUENCE)
      .orElse(java.time.Duration.ofMillis(CONSOLE_FREQUENCE_DEFAULT.toMillis))
      .toMillis milliseconds
  }

  override protected def terminate(): Unit = {
    // do nothing
  }

  override protected def putRecords(records: util.List[RowSinkRecord]): Unit =
    if (!records.isEmpty && (lastLog == -1 || CommonUtils.current() - lastLog >= freq.toMillis)) {
      try {
        LOG.info(records.asScala.map(_.row()).mkString(divider))
      } finally lastLog = CommonUtils.current()
    }
}
