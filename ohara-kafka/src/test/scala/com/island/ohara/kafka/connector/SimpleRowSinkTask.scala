package com.island.ohara.kafka.connector

import com.island.ohara.common.data.{Row, Serializer}
import com.island.ohara.common.util.CloseOnce
import com.island.ohara.kafka.Producer
import com.island.ohara.kafka.connector.Constants._

/**
  * Used for testing.
  */
class SimpleRowSinkTask extends RowSinkTask {
  private[this] var config: TaskConfig = _
  private[this] var outputTopic: String = _
  private[this] var producer: Producer[Array[Byte], Row] = _
  override def _start(props: TaskConfig): Unit = {
    this.config = props
    outputTopic = config.options(OUTPUT)
    producer = Producer.builder().brokers(config.options(BROKER)).build(Serializer.BYTES, Serializer.ROW)
  }

  override def _put(records: Seq[RowSinkRecord]): Unit = {
    records.foreach(r => {
      producer.sender().key(r.key).value(r.row).send(outputTopic)
    })
  }

  override def _stop(): Unit = CloseOnce.close(producer)
}
