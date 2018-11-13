package com.island.ohara.kafka.connector

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

import com.island.ohara.common.data.{Row, Serializer}
import com.island.ohara.client.util.CloseOnce
import com.island.ohara.kafka.Consumer
import com.island.ohara.kafka.connector.Constants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Used for testing.
  */
class SimpleRowSourceTask extends RowSourceTask {

  private[this] var config: TaskConfig = _
  private[this] val queue = new LinkedBlockingQueue[RowSourceRecord]
  private[this] val closed = new AtomicBoolean(false)
  private[this] var consumer: Consumer[Array[Byte], Row] = _
  override def _start(config: TaskConfig): Unit = {
    this.config = config
    this.consumer = Consumer
      .builder()
      .brokers(config.options(BROKER))
      .groupId(config.name)
      .topicName(config.options(INPUT))
      .offsetFromBegin()
      .build(Serializer.BYTES, Serializer.ROW)
    Future {
      try {
        while (!closed.get) {
          consumer
            .poll(2 seconds)
            .filter(_.value.isDefined)
            .map(_.value.get)
            .flatMap(row => config.topics.map(topic => RowSourceRecord.builder().row(row).build(topic)))
            .foreach(r => queue.put(r))
        }
      } finally CloseOnce.close(consumer)
    }
  }

  override def _poll(): Seq[RowSourceRecord] = Iterator.continually(queue.poll()).takeWhile(_ != null).toSeq

  override def _stop(): Unit = {
    closed.set(true)
    consumer.wakeup()
  }
}
