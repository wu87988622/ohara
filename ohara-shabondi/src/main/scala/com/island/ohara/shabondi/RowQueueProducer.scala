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

package com.island.ohara.shabondi

import java.time.Duration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

import com.island.ohara.common.data.{Row, Serializer}
import com.island.ohara.kafka.Consumer
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._
import scala.compat.java8.DurationConverters
import scala.concurrent.duration.FiniteDuration

private[shabondi] object RowQueueProducer {
  def apply(
    queue: BlockingQueue[Row],
    brokerProps: String,
    topicNames: Seq[String],
    pollTimeout: FiniteDuration,
    pollSize: Int
  ) =
    new RowQueueProducer(queue, brokerProps, topicNames, pollTimeout, pollSize)

  def apply(blockingQueue: BlockingQueue[Row], config: Config) =
    new RowQueueProducer(
      blockingQueue: BlockingQueue[Row],
      config.brokers,
      config.sinkFromTopics.map(_.name),
      config.sinkPollTimeout,
      config.sinkPollRowSize
    )
}

private[shabondi] class RowQueueProducer(
  val queue: BlockingQueue[Row],
  val brokerProps: String,
  val topicNames: Seq[String],
  val pollTimeout: FiniteDuration,
  val pollSize: Int
) extends Runnable
    with AutoCloseable {
  private[this] val log                       = Logger(classOf[RowQueueProducer])
  private[this] val paused: AtomicBoolean     = new AtomicBoolean(false)
  private[this] val stopped: AtomicBoolean    = new AtomicBoolean(false)
  private[this] val pollTimeoutJava: Duration = DurationConverters.toJava(pollTimeout)
  private[this] val limitSize                 = 50

  private[this] val consumer: Consumer[Row, Array[Byte]] = Consumer
    .builder()
    .keySerializer(Serializer.ROW)
    .valueSerializer(Serializer.BYTES)
    .offsetFromBegin()
    .topicNames(topicNames.asJava)
    .connectionProps(brokerProps)
    .build()

  override def run(): Unit = {
    log.debug("RowProducer start running: topics={}, brokerProps={}", topicNames.mkString(","), brokerProps)
    try {
      while (!stopped.get) {
        if (!paused.get) {
          if (queue.size < limitSize) {
            val rows: Seq[Row] = consumer.poll(pollTimeoutJava, pollSize).asScala.map(_.key.get)
            rows.foreach(r => queue.add(r))
            log.trace("  queue size: {}, row size: {}", queue.size, rows.size)
          }
        } else {
          log.trace("  paused. queue size: {}", queue.size)
        }
        Thread.sleep(50)
      } // while
    } finally {
      consumer.close()
      log.debug("{} stopped.", this.getClass.getSimpleName)
    }
  }

  override def close(): Unit = {
    stop()
  }

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
