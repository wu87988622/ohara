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

package oharastream.ohara.client

import java.util.concurrent.TimeUnit

import oharastream.ohara.common.data.{Cell, Row}
import oharastream.ohara.common.setting.{ConnectorKey, TopicKey}
import oharastream.ohara.common.util.CommonUtils
import oharastream.ohara.kafka.connector.TaskSetting

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

package object kafka {
  val ROW: Row = Row.of(Cell.of("f0", 13), Cell.of("f1", false))

  def result[T](f: Future[T]): T = Await.result(f, Duration(60, TimeUnit.SECONDS))

  def await(f: () => Boolean): Unit = CommonUtils.await(() => f(), java.time.Duration.ofSeconds(300))

  def assertExist(connectorAdmin: ConnectorAdmin, connectorKey: ConnectorKey): Boolean =
    CommonUtils.await(() => result(connectorAdmin.exist(connectorKey)) == true, java.time.Duration.ofSeconds(30))

  val OUTPUT = "simple.row.connector.output"
  val BROKER = "simple.row.connector.broker"
  val INPUT  = "simple.row.connector.input"

  def topicKey(settings: TaskSetting, key: String): TopicKey = TopicKey.toTopicKey(settings.stringValue(key))
}
