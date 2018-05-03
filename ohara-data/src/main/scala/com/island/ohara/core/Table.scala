package com.island.ohara.core

import scala.collection.{TraversableOnce, mutable}
import scala.collection.mutable.ArrayBuffer

abstract class Table extends Iterable[Row] {

  /**
    * @return id of table
    */
  def id: String

  /**
    * @return True if no cells exist in this table
    */
  def empty: Boolean = cellCount == 0

  /**
    * @return the number of cells
    */
  def cellCount: Int

  /**
    * @return the number of rows
    */
  def rowCount: Int

  /**
    * The origin behavior of size method is a expensive op.
    *
    * @return row count
    */
  override def size: Int = rowCount

  /**
    * seek the row at the specific index
    *
    * @param index row index
    * @return the row at the specific index
    * @throws IndexOutOfBoundsException if `i < 0` or `size <= i`
    */
  def seekRow(index: Int): Row

  /**
    * seek the cells having specific column name
    *
    * @param name column name
    * @return a collection of cells having specific column name
    */
  def seekCell(name: String): Iterator[Cell[_]]

  /**
    * Iterate the column names
    *
    * @return iterable of column name from all cells
    */
  def names: Iterator[String]
}

object Table {
  def builder(id: String): TableBuilder = new TableBuilderImpl(id)

  /**
    * Instantiate a table with copying all rows from passed argument
    */
  def apply(id: String, rows: Row*): Table = apply(id, rows)

  /**
    * Instantiate a row with a single row
    */
  def apply(id: String, row: Row): Table = apply(id, Array(row))

  /**
    * Instantiate a table with copying all rows from passed argument
    */
  def apply(_id: String, itor: TraversableOnce[Row]): Table = new Table() {
    private[this] val rowArray = itor.toArray

    override def rowCount: Int = rowArray.length

    override def iterator: Iterator[Row] = rowArray.iterator

    override def seekRow(index: Int): Row = try rowArray(index)
    catch {
      case e: ArrayIndexOutOfBoundsException => throw new IndexOutOfBoundsException(e.getMessage)
    }

    override def seekCell(name: String): Iterator[Cell[_]] = {
      val cells = new ArrayBuffer[Cell[_]]()
      rowArray.foreach(cells ++= _.filter(_.name eq name))
      cells.iterator
    }

    override def names: Iterator[String] = {
      val names = new mutable.TreeSet[String]
      rowArray.foreach(names ++= _.names)
      names.iterator
    }

    override def cellCount: Int = rowArray.map(_.size).sum

    /**
      * @return name of table
      */
    override def id: String = _id
  }
}
