package com.island.ohara.kafka.connector

import java.util
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.data.{Cell, Row}
import com.typesafe.scalalogging.Logger

import scala.collection.mutable.ArrayBuffer

/**
  * Used for testing.
  */
class SimpleRowSourceTask extends RowSourceTask {

  private[this] lazy val logger = Logger(getClass.getName)

  private[this] var topicName: String = null
  private[this] var pollCountMax: Int = -1

  override def start(props: util.Map[String, String]): Unit = {
    topicName = props.get("topic")
    pollCountMax = props.get(SimpleRowSourceConnector.POLL_COUNT_MAX).toInt
    logger.info(s"start SimpleRowSourceTask topicName:$topicName pollCount:$pollCountMax")
    if (pollCountMax <= 0) throw new IllegalArgumentException(s"count:$pollCountMax should be bigger than 0")
    SimpleRowSourceTask.runningTaskCount.incrementAndGet()
  }

  override def _poll(): Array[RowSourceRecord] = {
    if (SimpleRowSourceTask.pollCount.incrementAndGet() > pollCountMax) return null
    val data = new ArrayBuffer[RowSourceRecord]()
    SimpleRowSourceTask.rows.foreach(row => {
      data += new RowSourceRecord(null, null, topicName, row)
      SimpleRowSourceTask.submittedRows.add(row)
      logger.info(s"add row $row")
    })
    data.toArray
  }

  override def stop(): Unit = {
    logger.info("stop SimpleRowSourceTask")
    SimpleRowSourceTask.runningTaskCount.decrementAndGet()
  }

  override def version(): String = 100.toString
}

object SimpleRowSourceTask {
  def reset() = {
    runningTaskCount.set(0)
    submittedRows.clear()
    pollCount.set(0)
  }
  val rows: Array[Row] = Array(Row(Cell.builder.name("cf").build(1)),
                               Row(Cell.builder.name("cf").build(2)),
                               Row(Cell.builder.name("cf").build(3)))
  val runningTaskCount = new AtomicInteger(0)
  val submittedRows = new ConcurrentLinkedQueue[Row]
  val pollCount = new AtomicInteger(0)
}
