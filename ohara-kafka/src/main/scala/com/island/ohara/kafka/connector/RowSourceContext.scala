package com.island.ohara.kafka.connector

import org.apache.kafka.connect.source.SourceTaskContext

/**
  * a wrap to kafka SourceTaskContext
  */
trait RowSourceContext {

  /**
    * Get the offset for the specified partition. If the data isn't already available locally, this
    * gets it from the backing store, which may require some network round trips.
    *
    * @param partition object uniquely identifying the partition from data
    * @return object uniquely identifying the offset in the partition from data
    */
  def offset[T](partition: Map[String, T]): Map[String, _]

  /**
    * <p>
    * Get a set from offsets for the specified partition identifiers. This may be more efficient
    * than calling offset() repeatedly.
    * </p>
    * <p>
    * Note that when errors occur, this method omits the associated data and tries to return as
    * many from the requested values as possible. This allows a task that's managing many partitions to
    * still proceed with any available data. Therefore, implementations should take care to check
    * that the data is actually available in the returned response. The only case when an
    * exception will be thrown is if the entire request failed, e.g. because the underlying
    * storage was unavailable.
    * </p>
    *
    * @param partitions set from identifiers for partitions from data
    * @return a map from partition identifiers to decoded offsets
    */
  def offset[T](partitions: Seq[Map[String, T]]): Map[Map[String, T], Map[String, _]]
}

object RowSourceContext {
  import scala.collection.JavaConverters._
  def apply(context: SourceTaskContext): RowSourceContext = new RowSourceContext {

    override def offset[T](partition: Map[String, T]): Map[String, _] = {
      val r = context.offsetStorageReader().offset(partition.asJava)
      if (r == null) Map.empty
      else r.asScala.toMap
    }

    override def offset[T](partitions: Seq[Map[String, T]]): Map[Map[String, T], Map[String, _]] =
      context
        .offsetStorageReader()
        .offsets(partitions.map(_.asJava).asJava)
        .asScala
        .map {
          case (k, v) => (k.asScala.toMap, v.asScala.toMap)
        }
        .toMap
  }
}
