package com.island.ohara.kafka.connector

import java.util

import org.apache.kafka.connect.sink.{SinkRecord, SinkTask}

import scala.collection.mutable.ArrayBuffer

/**
  * A wrap to Kafka SinkTask. It used to convert the Kafka SinkRecord to ohara RowSinkRecord.
  * Ohara developers should extend this class rather than kafka SinkTask in order to let the conversion from SinkRecord to
  * RowSinkRecord work automatically.
  */
abstract class RowSinkTask extends SinkTask {

  /**
    * Put the table record in the sink. Usually this should send the records to the sink asynchronously
    * and immediately return.
    *
    * @param records table record
    */
  protected def _put(records: Array[RowSinkRecord]): Unit

  /**
    * This method is overrided to convert the kafka object to ohara object.
    *
    * @param records kafka sink record
    */
  final override def put(records: util.Collection[SinkRecord]): Unit = {
    if (records == null) _put(Array[RowSinkRecord]())
    else {
      val buf = new ArrayBuffer[RowSinkRecord](records.size)
      records.forEach(record => buf += new RowSinkRecord(record))
      _put(buf.toArray)
    }
  }
}
