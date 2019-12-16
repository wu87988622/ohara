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
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue, ExecutorService, Executors, TimeUnit}
import java.util.{Queue => JQueue}

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.island.ohara.common.data.{Row, Serializer}
import com.island.ohara.common.util.Releasable
import com.island.ohara.kafka.Consumer
import com.island.ohara.shabondi.Config
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._

private[sink] class DataGroup(val name: String, brokerProps: String, topicNames: Seq[String], pollTimeout: Duration)
    extends Releasable {
  val queue    = new ConcurrentLinkedQueue[Row]
  val producer = new QueueProducer(name, queue, brokerProps, topicNames, pollTimeout)

  private val lastTime = new AtomicLong(0)

  def updateLastTime(): Unit = {
    lastTime.set(System.currentTimeMillis())
  }

  def idleTime(): Long = System.currentTimeMillis() - lastTime.get()

  def resume(): Unit = {
    producer.resume()
  }

  def pause(): Unit = {
    producer.pause()
  }

  override def close(): Unit = {
    producer.close()
  }
}

private[sink] object SinkDataGroups {
  def apply(config: Config) =
    new SinkDataGroups(config.brokers, config.sinkFromTopics.map(_.name()), config.sinkPollTimeout)
}

private[sink] class SinkDataGroups(brokerProps: String, topicNames: Seq[String], pollTimeout: Duration)
    extends Releasable {
  private val threadPool: ExecutorService =
    Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("SinkDataGroups-%d").build())

  private val log              = Logger(classOf[SinkDataGroups])
  private val defaultGroupName = "__dafault__"
  private val dataGroups       = new ConcurrentHashMap[String, DataGroup]()

  def defaultGroup: DataGroup = createIfAbsent(defaultGroupName)

  def removeGroup(name: String): Boolean = {
    if (groupExist(name)) {
      val dataGroup = dataGroups.remove(name)
      dataGroup.close()
      true
    } else false
  }

  def groupExist(name: String): Boolean =
    dataGroups.containsKey(name)

  def createIfAbsent(name: String): DataGroup =
    dataGroups.computeIfAbsent(name, { n =>
      log.info("create group: {}", n)
      val dataGroup = new DataGroup(n, brokerProps, topicNames, pollTimeout)
      threadPool.submit(dataGroup.producer)
      dataGroup
    })

  def size: Int = dataGroups.size()

  override def close(): Unit = {
    dataGroups.asScala.foreach {
      case (_, dataGroup) =>
        dataGroup.close()
    }
    threadPool.shutdown()
  }
}

private[sink] class QueueProducer(
  val groupName: String,
  val queue: JQueue[Row],
  val brokerProps: String,
  val topicNames: Seq[String],
  val pollTimeout: Duration
) extends Runnable
    with Releasable {
  private[this] val log                    = Logger(classOf[QueueProducer])
  private[this] val paused: AtomicBoolean  = new AtomicBoolean(false)
  private[this] val stopped: AtomicBoolean = new AtomicBoolean(false)

  private[this] val consumer: Consumer[Row, Array[Byte]] = Consumer
    .builder()
    .keySerializer(Serializer.ROW)
    .valueSerializer(Serializer.BYTES)
    .offsetFromBegin()
    .topicNames(topicNames.asJava)
    .connectionProps(brokerProps)
    .build()

  override def run(): Unit = {
    log.info(
      "{} group `{}` start.(topics={}, brokerProps={})",
      this.getClass.getSimpleName,
      groupName,
      topicNames.mkString(","),
      brokerProps
    )
    try {
      while (!stopped.get) {
        if (!paused.get && queue.isEmpty) {
          val rows: Seq[Row] = consumer.poll(pollTimeout).asScala.map(_.key.get)
          rows.foreach(r => queue.add(r))
          log.trace("    group[{}], queue: {}, rows: {}", groupName, queue.size, rows.size)
        } else {
          TimeUnit.MILLISECONDS.sleep(10)
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

  def stop(): Unit = {
    stopped.set(true)
  }

  def pause(): Unit = {
    if (paused.compareAndSet(false, true)) {
      log.info("{} paused.", this.getClass.getSimpleName)
    }
  }

  def resume(): Unit = {
    if (paused.compareAndSet(true, false)) {
      log.info("{} resumed.", this.getClass.getSimpleName)
    }
  }
}
