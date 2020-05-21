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

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import java.util.concurrent.{ArrayBlockingQueue, Executors, LinkedBlockingDeque, TimeUnit}

import oharastream.ohara.client.configurator.v0.{BrokerApi, TopicApi}
import oharastream.ohara.common.setting.TopicKey
import oharastream.ohara.common.util.Releasable
import oharastream.ohara.testing.WithBrokerWorker
import org.junit.{After, Test}
import org.scalatest.matchers.should.Matchers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestDeleteTopicsConcurrently extends WithBrokerWorker {
  private[this] val configurator =
    Configurator.builder.fake(testUtil.brokersConnProps, testUtil().workersConnProps()).build()

  private[this] val topicApi = TopicApi.access.hostname(configurator.hostname).port(configurator.port)

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  private[this] val brokerClusterInfo = result(
    BrokerApi.access.hostname(configurator.hostname).port(configurator.port).list()
  ).head

  @Test
  def test(): Unit = {
    val loopMax       = 30
    val count         = 5
    val topicKeyQueue = new ArrayBlockingQueue[TopicKey](count)
    (0 until count).foreach(i => topicKeyQueue.put(TopicKey.of("test", i.toString)))
    val executors      = Executors.newFixedThreadPool(count)
    val exceptionQueue = new LinkedBlockingDeque[Throwable]()
    val closed         = new AtomicBoolean(false)
    val loopCount      = new AtomicInteger(0)
    (0 until count).foreach(
      _ =>
        executors.execute { () =>
          while (!closed.get() && loopCount.getAndIncrement() <= loopMax) try {
            val topicKey = topicKeyQueue.take()
            try result(
              topicApi.request
                .group(topicKey.group())
                .name(topicKey.name())
                .brokerClusterKey(brokerClusterInfo.key)
                .numberOfPartitions(1)
                .numberOfReplications(1)
                .create()
                .flatMap(_ => topicApi.start(topicKey))
                .flatMap { _ =>
                  TimeUnit.SECONDS.sleep(1)
                  topicApi.stop(topicKey)
                }
                .flatMap { _ =>
                  TimeUnit.SECONDS.sleep(1)
                  topicApi.delete(topicKey)
                }
            )
            finally topicKeyQueue.put(topicKey)
          } catch {
            case t: Throwable =>
              exceptionQueue.put(t)
              closed.set(true)
          }
        }
    )
    executors.shutdown()
    executors.awaitTermination(30, TimeUnit.SECONDS) shouldBe true
    exceptionQueue.size() shouldBe 0
  }
  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
