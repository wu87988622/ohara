package com.island.ohara.kafka.connector

import java.util
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.core.{Cell, Row}
import com.typesafe.scalalogging.Logger

import scala.collection.mutable.ArrayBuffer

/**
  * Used for testing.
  */
class SimpleRowSourceTask extends RowSourceTask {

  private[this] lazy val logger = Logger(getClass.getName)

  private[this] var topicName: String = null
  private[this] var count: Int = -1

  override def start(props: util.Map[String, String]): Unit = {
    topicName = props.get("topic")
    count = props.get("task.count").toInt
    logger.info(s"start SimpleRowSourceTask topicName:$topicName count:$count")
    if (count <= 0) throw new IllegalArgumentException(s"count:$count should be bigger than 0")
  }

  override def _poll(): Array[RowSourceRecord] = {
    if (SimpleRowSourceTask.taskCount.incrementAndGet() > count) return null
    val data = new ArrayBuffer[RowSourceRecord]()
    SimpleRowSourceTask.dataSet.foreach(value => {
      data += new RowSourceRecord(null, null, topicName, Row(Cell.builder.name("cf").build(value)))
      SimpleRowSourceTask.taskValues.add(value)
      logger.info(s"add $value")
    })
    data.toArray
  }

  override def stop(): Unit = {
    logger.info("stop SimpleRowSourceTask")
  }

  override def version(): String = 100.toString
}

object SimpleRowSourceTask {
  val dataSet: Seq[Int] = Array[Int](1, 2, 3)
  val taskValues = new ConcurrentLinkedQueue[Int]
  val taskCount = new AtomicInteger(0)
}
