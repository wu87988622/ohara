package com.island.ohara.integration

import java.util
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

import com.typesafe.scalalogging.Logger
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.source.{SourceRecord, SourceTask}

/**
  * Used for testing.
  */
class SimpleSourceTask extends SourceTask {

  private[this] lazy val logger = Logger(getClass.getName)

  private[this] var topicName: String = null
  private[this] var count: Int = -1

  override def start(props: util.Map[String, String]): Unit = {
    topicName = props.get("topic")
    count = props.get("task.count").toInt
    logger.info(s"start SimpleSourceTask topicName:$topicName count:$count")
    if (count <= 0) throw new IllegalArgumentException(s"count:$count should be bigger than 0")
  }

  override def poll(): util.List[SourceRecord] = {
    if (SimpleSourceTask.taskCount.incrementAndGet() > count) return null
    val data = new util.ArrayList[SourceRecord]()
    SimpleSourceTask.dataSet.foreach(value => {
      data.add(new SourceRecord(null, null, topicName, null, null, Schema.INT32_SCHEMA, value))
      SimpleSourceTask.taskValues.add(value)
      logger.info(s"add $value")
    })
    data
  }

  override def stop(): Unit = {
    logger.info("stop SimpleSourceTask")
  }

  override def version(): String = 100.toString
}

object SimpleSourceTask {
  val dataSet: Seq[Int] = Array[Int](1, 2, 3)
  val taskValues = new ConcurrentLinkedQueue[Int]
  val taskCount = new AtomicInteger(0)
}