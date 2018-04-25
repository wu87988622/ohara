package com.island.ohara.kafka.connector

import com.island.ohara.core.Table
import com.island.ohara.serialization.{TableReader, TableWriter}
import org.apache.kafka.connect.data.Schema

/**
  * A utils class. It stores, currently, the kafka schema constants. Make it private since it is used internally.
  */
private object ConnectSchema {
  def TABLE_SCHEMA = Schema.BYTES_SCHEMA

  def ROW_SCHEMA = Schema.BYTES_SCHEMA
}
