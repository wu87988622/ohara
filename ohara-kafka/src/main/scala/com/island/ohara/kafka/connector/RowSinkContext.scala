package com.island.ohara.kafka.connector
import org.apache.kafka.connect.sink.SinkTaskContext

/**
  * a wrap to kafka SinkTaskContext
  */
trait RowSinkContext {

  /**
    * Reset the consumer offsets for the given topic partitions. SinkTasks should use this if they manage offsets
    * in the sink data store rather than using Kafka consumer offsets. For example, an HDFS connector might record
    * offsets in HDFS to provide exactly once delivery. When the SinkTask is started or a rebalance occurs, the task
    * would reload offsets from HDFS and use this method to reset the consumer to those offsets.
    *
    * SinkTasks that do not manage their own offsets do not need to use this method.
    *
    * @param offsets map from offsets for topic partitions
    */
  def offset(offsets: Map[TopicPartition, Long]): Unit

  /**
    * Reset the consumer offsets for the given topic partition. SinkTasks should use if they manage offsets
    * in the sink data store rather than using Kafka consumer offsets. For example, an HDFS connector might record
    * offsets in HDFS to provide exactly once delivery. When the topic partition is recovered the task
    * would reload offsets from HDFS and use this method to reset the consumer to the offset.
    *
    * SinkTasks that do not manage their own offsets do not need to use this method.
    *
    * @param partition the topic partition to reset offset.
    * @param offset the offset to reset to.
    */
  def offset(partition: TopicPartition, offset: Long): Unit = this.offset(Map(partition -> offset))
}

object RowSinkContext {
  import scala.collection.JavaConverters._
  def apply(context: SinkTaskContext): RowSinkContext = new RowSinkContext {

    override def offset(offsets: Map[TopicPartition, Long]): Unit = context.offset(offsets.map {
      case (p, o) => (new org.apache.kafka.common.TopicPartition(p.topic, p.partition), new java.lang.Long(o))
    }.asJava)
  }
}
