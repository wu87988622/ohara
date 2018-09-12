package com.island.ohara.connector.jdbc.source

import java.sql.{PreparedStatement, ResultSet}
import com.island.ohara.io.CloseOnce

class QueryResultIterator(preparedStatement: PreparedStatement, columns: Seq[String])
    extends Iterator[Seq[Object]]
    with CloseOnce {
  private[this] val resultSet: ResultSet = preparedStatement.executeQuery()
  private[this] var cache: Seq[Object] = null

  def hasNext(): Boolean = {
    if (cache == null) {
      if (resultSet.next()) {
        //TODO ResultSet converter dataType OHARA-412
        cache = columns.map(resultSet.getString).toSeq
        true
      } else {
        false
      }
    } else {
      true
    }
  }

  def next(): Seq[Object] = {
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
