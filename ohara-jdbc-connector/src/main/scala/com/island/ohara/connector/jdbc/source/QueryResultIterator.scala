package com.island.ohara.connector.jdbc.source

import java.sql.{PreparedStatement, ResultSet}

import com.island.ohara.client.ConfiguratorJson.RdbColumn
import com.island.ohara.connector.jdbc.util.ColumnInfo
import com.island.ohara.io.CloseOnce

class QueryResultIterator(preparedStatement: PreparedStatement, columns: Seq[RdbColumn])
    extends Iterator[Seq[ColumnInfo]]
    with CloseOnce {
  private[this] val resultSet: ResultSet = preparedStatement.executeQuery()
  private[this] var cache: Seq[ColumnInfo] = null

  override def hasNext(): Boolean = {
    if (cache == null) {
      if (resultSet.next()) {
        cache = ResultSetDataConverter.converterRecord(resultSet, columns)
        true
      } else {
        false
      }
    } else {
      true
    }
  }

  override def next(): Seq[ColumnInfo] = {
    if (hasNext() == false) throw new NoSuchElementException("Cache no data")
    else {
      try {
        cache
      } finally cache = null
    }
  }

  /**
    * Do what you want to do when calling closing.
    */
  override protected def doClose(): Unit = {
    preparedStatement.close()
  }
}
