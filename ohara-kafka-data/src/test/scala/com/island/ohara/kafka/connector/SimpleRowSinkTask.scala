package com.island.ohara.kafka.connector

import java.util
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

import com.typesafe.scalalogging.Logger

/**
  * Used for testing.
  */
class SimpleRowSinkTask extends RowSinkTask {

  private[this] lazy val logger = Logger(getClass.getName)

  override def start(props: util.Map[String, String]): Unit = SimpleRowSinkTask.taskCount.incrementAndGet()

  override def _put(records: Array[RowSinkRecord]): Unit = {
    records.foreach(r => {
      r.value.foreach(cell => {
        logger.info(s"get ${cell.value}")
        SimpleRowSinkTask.taskValues.add(cell.value.asInstanceOf[Int])
      })

    })
  }

  override def stop(): Unit = {
    logger.info("stop SimpleSinkTask")
  }

  override def version(): String = 100.toString
}

object SimpleRowSinkTask {
  val taskValues = new ConcurrentLinkedQueue[Int]
  val taskCount = new AtomicInteger(0)
}
