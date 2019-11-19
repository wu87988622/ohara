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

package com.island.ohara.configurator.validation

import com.island.ohara.client.configurator.v0.WorkerApi
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.Releasable
import com.island.ohara.configurator.Configurator
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.After

import scala.concurrent.ExecutionContext.Implicits.global

private[validation] abstract class WithConfigurator extends With3Brokers3Workers {
  private[this] val configurator =
    Configurator.builder.fake(testUtil().brokersConnProps(), testUtil().workersConnProps()).build()
  private[this] val workerCluster = result(
    WorkerApi.access.hostname(configurator.hostname).port(configurator.port).list()
  ).head
  protected val workerClusterKey: ObjectKey  = workerCluster.key
  protected val configuratorHostname: String = configurator.hostname
  protected val configuratorPort: Int        = configurator.port

  @After
  def tearDown(): Unit =
    Releasable.close(configurator)
}
