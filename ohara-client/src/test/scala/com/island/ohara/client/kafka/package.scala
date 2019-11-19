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

package com.island.ohara.client

import java.time.Duration

import com.island.ohara.common.data.{Cell, Row}
import com.island.ohara.common.setting.ConnectorKey
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
package object kafka {
  val ROW: Row = Row.of(Cell.of("f0", 13), Cell.of("f1", false))

  def result[T](f: Future[T]): T = Await.result(f, 60 seconds)

  def await(f: () => Boolean): Unit = CommonUtils.await(() => f(), Duration.ofSeconds(300))

  def assertExist(workerClient: WorkerClient, connectorKey: ConnectorKey): Boolean =
    CommonUtils.await(() => result(workerClient.exist(connectorKey)) == true, java.time.Duration.ofSeconds(30))

  val OUTPUT = "simple.row.connector.output"
  val BROKER = "simple.row.connector.broker"
  val INPUT  = "simple.row.connector.input"
}
