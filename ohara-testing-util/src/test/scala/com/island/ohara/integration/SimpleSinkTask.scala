package com.island.ohara.integration

import java.util
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

import com.typesafe.scalalogging.Logger
import org.apache.kafka.connect.sink.{SinkRecord, SinkTask}

/**
  * Used for testing.
  */
class SimpleSinkTask extends SinkTask {

  private[this] lazy val logger = Logger(getClass.getName)

  override def start(props: util.Map[String, String]): Unit = SimpleSinkTask.taskCount.incrementAndGet()

  override def put(records: util.Collection[SinkRecord]): Unit = {
    records.forEach(r => {
      val value: Int = r.value().asInstanceOf[Int]
      logger.info(s"get $value")
      SimpleSinkTask.taskValues.add(value)
    })
  }

  override def stop(): Unit = {
    logger.info("stop SimpleSinkTask")
  }

  override def version(): String = 100.toString
}

object SimpleSinkTask {
  val taskValues = new ConcurrentLinkedQueue[Int]
  val taskCount = new AtomicInteger(0)
}
