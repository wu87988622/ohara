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

package com.island.ohara.shabondi.sink

import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import com.island.ohara.common.data.{Row, Serializer}
import com.island.ohara.kafka.Consumer
import com.island.ohara.shabondi.Config
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._

private[shabondi] object RowQueueProducer {
  def apply(config: Config) =
    new RowQueueProducer(config.brokers, config.sinkFromTopics.map(_.name), config.sinkPollTimeout)
}

private[shabondi] class RowQueueProducer(
  val brokerProps: String,
  val topicNames: Seq[String],
  val pollTimeout: Duration
) extends Runnable
    with AutoCloseable {
  private[this] val log                    = Logger(classOf[RowQueueProducer])
  private[this] val paused: AtomicBoolean  = new AtomicBoolean(false)
  private[this] val stopped: AtomicBoolean = new AtomicBoolean(false)
  private[this] val blockingQueue          = new LinkedBlockingQueue[Row]()

  private[this] val consumer: Consumer[Row, Array[Byte]] = Consumer
    .builder()
    .keySerializer(Serializer.ROW)
    .valueSerializer(Serializer.BYTES)
    .offsetFromBegin()
    .topicNames(topicNames.asJava)
    .connectionProps(brokerProps)
    .build()

  override def run(): Unit = {
    log.debug("{} start running.", this.getClass.getSimpleName)
    log.debug("topics={}, brokerProps={}", topicNames.mkString(","), brokerProps)
    try {
      while (!stopped.get) {
        if (!paused.get && blockingQueue.isEmpty) {
          val rows: Seq[Row] = consumer.poll(pollTimeout).asScala.map(_.key.get)
          rows.foreach(r => blockingQueue.add(r))
          log.trace("  queue size: {}, row size: {}", blockingQueue.size, rows.size)
        } else {
          log.trace("  paused or not empty. queue size: {}", blockingQueue.size)
          Thread.sleep(10)
        }
      } // while
    } finally {
      consumer.close()
      log.debug("{} stopped.", this.getClass.getSimpleName)
    }
  }

  override def close(): Unit = {
    stop()
  }

  def queue: BlockingQueue[Row] = blockingQueue

  def stop(): Unit = {
    stopped.set(true)
  }

  def pause(): Unit = {
    paused.set(true)
    log.info("{} paused.", this.getClass.getSimpleName)
  }

  def resume(): Unit = {
    paused.set(false)
    log.info("{} resumed.", this.getClass.getSimpleName)
  }
}
