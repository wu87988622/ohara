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

package com.island.ohara.connector.jio

import com.island.ohara.kafka.connector.TaskSetting

import scala.concurrent.duration._
import scala.compat.java8.OptionConverters._
case class JioProps(bufferSize: Int,
                    closeTimeout: FiniteDuration,
                    bindingTimeout: FiniteDuration,
                    bindingPort: Int,
                    bindingPath: String) {
  def plain: Map[String, String] = Map(
    DATA_BUFFER_KEY -> bufferSize.toString,
    CLOSE_TIMEOUT_KEY -> java.time.Duration.ofMillis(closeTimeout.toMillis).toString,
    BINDING_TIMEOUT_KEY -> java.time.Duration.ofMillis(bindingTimeout.toMillis).toString,
    BINDING_PORT_KEY -> bindingPort.toString,
    BINDING_PATH_KEY -> bindingPath
  )
}

object JioProps {
  def apply(bindingPort: Int): JioProps = JioProps(
    bufferSize = DATA_BUFFER_DEFAULT,
    closeTimeout = CLOSE_TIMEOUT_DEFAULT,
    bindingTimeout = BINDING_TIMEOUT_DEFAULT,
    bindingPort = bindingPort,
    bindingPath = BINDING_PATH_DEFAULT
  )

  def apply(setting: TaskSetting): JioProps = JioProps(
    bufferSize = setting.intOption(DATA_BUFFER_KEY).orElse(DATA_BUFFER_DEFAULT),
    closeTimeout = setting
      .durationOption(CLOSE_TIMEOUT_KEY)
      .asScala
      .map(t => t.toMillis milliseconds)
      .getOrElse(CLOSE_TIMEOUT_DEFAULT),
    bindingTimeout = setting
      .durationOption(BINDING_TIMEOUT_KEY)
      .asScala
      .map(t => t.toMillis milliseconds)
      .getOrElse(BINDING_TIMEOUT_DEFAULT),
    bindingPort = setting.intValue(BINDING_PORT_KEY),
    bindingPath = setting.stringOption(BINDING_PATH_KEY).orElse(BINDING_PATH_DEFAULT)
  )
}
