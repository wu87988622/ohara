package com.island.ohara.connector.jdbc.source

import java.sql.{PreparedStatement, ResultSet}
import com.island.ohara.client.ConfiguratorJson.RdbColumn
import com.island.ohara.connector.jdbc.util.ColumnInfo
import com.island.ohara.io.CloseOnce

class QueryResultIterator(preparedStatement: PreparedStatement, columns: Seq[RdbColumn])
    extends Iterator[Seq[ColumnInfo[_]]]
    with CloseOnce {
  private[this] val resultSet: ResultSet = preparedStatement.executeQuery()
  private[this] var cache: Seq[ColumnInfo[_]] = _

  /**
    * this method bring side effect the first time since we have to "touch" remote db to retrieve the "data information"
    * to check the existence of data...
    * @return true if there are some data. otherwise false
    */
  override def hasNext: Boolean = {
    if (cache == null && resultSet.next()) cache = ResultSetDataConverter.converterRecord(resultSet, columns)
    cache != null
  }

  override def next(): Seq[ColumnInfo[_]] = {
    if (!hasNext) throw new NoSuchElementException("Cache no data")
    else
      try cache
      finally cache = null
  }

  /**
    * Do what you want to do when calling closing.
    */
  override protected def doClose(): Unit = {
    preparedStatement.close()
  }
}
