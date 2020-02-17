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

package oharastream.ohara.configurator

import oharastream.ohara.client.configurator.v0.{InspectApi, WorkerApi}
import oharastream.ohara.common.util.Releasable
import oharastream.ohara.testing.WithBrokerWorker
import org.junit.{After, Test}
import org.scalatest.Matchers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestInspectWorkerCluster extends WithBrokerWorker {
  private[this] val configurator =
    Configurator.builder.fake(testUtil().brokersConnProps(), testUtil().workersConnProps()).build()

  private[this] val workerClusterInfo = result(
    WorkerApi.access.hostname(configurator.hostname).port(configurator.port).list()
  ).head
  private[this] def inspectApi = InspectApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 20 seconds)

  @Test
  def inspectWithoutKey(): Unit = {
    val info = result(inspectApi.workerInfo())
    info.imageName shouldBe WorkerApi.IMAGE_NAME_DEFAULT
    info.settingDefinitions.size shouldBe WorkerApi.DEFINITIONS.size
    info.settingDefinitions.foreach { definition =>
      definition shouldBe WorkerApi.DEFINITIONS.find(_.key() == definition.key()).get
    }
  }

  @Test
  def inspectWithKey(): Unit = {
    val info = result(inspectApi.workerInfo(workerClusterInfo.key))
    info.imageName shouldBe WorkerApi.IMAGE_NAME_DEFAULT
    info.settingDefinitions.size shouldBe WorkerApi.DEFINITIONS.size
    info.settingDefinitions.foreach { definition =>
      definition shouldBe WorkerApi.DEFINITIONS.find(_.key() == definition.key()).get
    }
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
