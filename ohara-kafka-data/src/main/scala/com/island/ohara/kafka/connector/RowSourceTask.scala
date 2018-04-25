package com.island.ohara.kafka.connector

import java.util

import org.apache.kafka.connect.source.{SourceRecord, SourceTask}

import scala.collection.JavaConverters._

/**
  * A wrap to SourceTask. The users should substitute RowSourceRecord for SourceRecord.
  */
abstract class RowSourceTask extends SourceTask {

  /**
    * Poll this SourceTask for new records. This method should block if no data is currently available.
    *
    * @return a array of RowSourceRecord
    */
  protected def _poll(): Array[RowSourceRecord]

  /**
    * We take over this method to enforce ohara-XXX code to be used
    */
  final override def poll(): util.List[SourceRecord] = _poll().map(_.asInstanceOf[SourceRecord]).toList.asJava
}
