package com.island.ohara.kafka.connector

import com.island.ohara.data.Row

/**
  * The methods it have are almost same with SinkRecord.
  * It return Table rather than any object. Also, it doesn't have method to return value schema
  * because the value schema is useless to user.
  *
  * @param sinkRecord a sink record passed by kafka connector
  */
case class RowSinkRecord(topic: String,
                         key: Array[Byte],
                         row: Row,
                         partition: Int,
                         offset: Long,
                         timestamp: Long,
                         timestampType: TimestampType)

case class TimestampType(id: Int, name: String)
