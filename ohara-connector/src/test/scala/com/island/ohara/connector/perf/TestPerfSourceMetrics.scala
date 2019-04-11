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

import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.metrics.BeanChannel
import com.island.ohara.testing.WithBrokerWorker
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
class TestPerfSourceMetrics extends WithBrokerWorker with Matchers {
  private[this] val workerClient = WorkerClient(testUtil.workersConnProps)

  private[this] val props = PerfSourceProps(
    batch = 5,
    freq = 5 seconds
  )

  @Test
  def test(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    Await.result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[PerfSource])
        .numberOfTasks(1)
        .name(connectorName)
        .settings(props.toMap)
        .create,
      10 seconds
    )
    CommonUtils.await(() => {
      !BeanChannel.local().counterMBeans().isEmpty
    }, java.time.Duration.ofSeconds(30))
    val counters = BeanChannel.local().counterMBeans()
    counters.size should not be 0
    counters.asScala.foreach { counter =>
      counter.getValue should not be 0
      counter.getStartTime should not be 0
      CommonUtils.requireNonEmpty(counter.getUnit)
      CommonUtils.requireNonEmpty(counter.getDocument)
    }
  }
}
