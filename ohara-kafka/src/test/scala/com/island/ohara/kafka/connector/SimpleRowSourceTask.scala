package com.island.ohara.kafka.connector

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

  private[this] var topicName: String = _
  private[this] var pollCountMax: Int = -1

  override def _start(config: TaskConfig): Unit = {
    topicName = config.topics.head
    pollCountMax = config.options.get(SimpleRowSourceConnector.POLL_COUNT_MAX).map(_.toInt).get
    logger.info(s"start SimpleRowSourceTask topicName:$topicName pollCount:$pollCountMax")
    if (pollCountMax <= 0) throw new IllegalArgumentException(s"count:$pollCountMax should be bigger than 0")
    SimpleRowSourceTask.runningTaskCount.incrementAndGet()
  }

  override def _poll(): Seq[RowSourceRecord] = {
    if (SimpleRowSourceTask.pollCount.incrementAndGet() > pollCountMax) return null
    val data = new ArrayBuffer[RowSourceRecord]()
    SimpleRowSourceTask.rows.foreach(row => {
      data += RowSourceRecord(topicName, row)
      SimpleRowSourceTask.submittedRows.add(row)
      logger.info(s"add row $row")
    })
    data
  }

  override def _stop(): Unit = {
    logger.info("stop SimpleRowSourceTask")
    SimpleRowSourceTask.runningTaskCount.decrementAndGet()
  }

  override val _version: String = 100.toString
}

object SimpleRowSourceTask {
  def reset(): Unit = {
    runningTaskCount.set(0)
    submittedRows.clear()
    pollCount.set(0)
  }
  val rows: Seq[Row] = Seq(Row(Cell("cf", 1)), Row(Cell("cf", 2)), Row(Cell("cf", 3)))
  val runningTaskCount = new AtomicInteger(0)
  val submittedRows = new ConcurrentLinkedQueue[Row]
  val pollCount = new AtomicInteger(0)
}
