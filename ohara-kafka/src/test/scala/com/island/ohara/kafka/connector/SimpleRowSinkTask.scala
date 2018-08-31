package com.island.ohara.kafka.connector

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.data.Row
import com.typesafe.scalalogging.Logger

/**
  * Used for testing.
  */
class SimpleRowSinkTask extends RowSinkTask {

  private[this] lazy val logger = Logger(getClass.getName)

  override def _start(props: Map[String, String]): Unit = SimpleRowSinkTask.runningTaskCount.incrementAndGet()

  override def _put(records: Seq[RowSinkRecord]): Unit = {
    records
      .map(_.row)
      .foreach(row => {
        logger.info(s"get $row")
        SimpleRowSinkTask.receivedRows.add(row)
      })
  }

  override def _stop(): Unit = {
    logger.info("stop SimpleSinkTask")
    SimpleRowSinkTask.runningTaskCount.decrementAndGet()
  }

  override val _version: String = 100.toString
}

object SimpleRowSinkTask {
  def reset(): Unit = {
    receivedRows.clear()
    runningTaskCount.set(0)
  }
  val receivedRows = new ConcurrentLinkedQueue[Row]
  val runningTaskCount = new AtomicInteger(0)
}
