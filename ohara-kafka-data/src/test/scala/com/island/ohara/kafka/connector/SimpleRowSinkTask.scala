package com.island.ohara.kafka.connector

import java.util
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.core.Row
import com.typesafe.scalalogging.Logger

/**
  * Used for testing.
  */
class SimpleRowSinkTask extends RowSinkTask {

  private[this] lazy val logger = Logger(getClass.getName)

  override def start(props: util.Map[String, String]): Unit = SimpleRowSinkTask.runningTaskCount.incrementAndGet()

  override def _put(records: Array[RowSinkRecord]): Unit = {
    records
      .map(_.value)
      .foreach(row => {
        logger.info(s"get ${row}")
        SimpleRowSinkTask.receivedRows.add(row)
      })
  }

  override def stop(): Unit = {
    logger.info("stop SimpleSinkTask")
    SimpleRowSinkTask.runningTaskCount.decrementAndGet()
  }

  override def version(): String = 100.toString
}

object SimpleRowSinkTask {
  def reset() = {
    receivedRows.clear()
    runningTaskCount.set(0)
  }
  val receivedRows = new ConcurrentLinkedQueue[Row]
  val runningTaskCount = new AtomicInteger(0)
}
