package com.island.ohara.data

import scala.collection.mutable.ArrayBuffer
import scala.collection.{TraversableOnce, mutable}

abstract class Row extends Iterable[Cell[_]] {

  /**
    * Iterate the column names
    *
    * @return iterable of column name from all cells
    */
  def names: Iterator[String]

  /**
    * seek the cell having the specific column name
    *
    * @param name cell name
    * @return None if the target cell doesn't exist
    */
  def seekCell(name: String): Option[Cell[_]]

  /**
    * seek the cell at the specific index
    *
    * @param index cell index
    * @return the cell at the specific index
    * @throws IndexOutOfBoundsException if `i < 0` or `size <= i`
    */
  def seekCell(index: Int): Cell[_]

  /**
    * @return the number of cells
    */
  def cellCount: Int

  /**
    * The origin behavior of size method is a expensive op.
    *
    * @return row count
    */
  override def size: Int = cellCount

  override def toString(): String = toList.mkString(",")

  /**
    * Indicates whether this row is equal to another row
    * NOTED: the default implementation depends on the Cell#quals.
    *
    * @param obj another row
    * @return true if this row is equal with another row. false otherwise
    */
  override def equals(obj: scala.Any): Boolean = obj match {
    case row: Row => equals(row, true)
    case _        => false
  }

  def equals(other: Row, includeTag: Boolean = true): Boolean =
    compareCell(other) && (!includeTag || compareTags(other))

  // TODO: evaluating the size first may be reduce the performance if the Row impl get the size by iterating. by chia
  private[this] def compareCell(other: Row): Boolean = if (isEmpty && other.isEmpty) true
  else if (size == other.size) !filter(c => other.seekCell(c.name).map(_.equals(c)).getOrElse(false)).isEmpty
  else false

  private[this] def compareTags(other: Row): Boolean = if (tags.isEmpty && other.tags.isEmpty) true
  else if (tags.size == other.tags.size) !tags.filter(tag => other.tags.contains(tag)).isEmpty
  else false
  def tags: Set[String]
}

object Row {
  def builder: RowBuilder = new RowBuilderImpl()

  /**
    * Instantiate a row with a single cell
    */
  def apply(cell: Cell[_]): Row = apply(Array(cell))

  /**
    * Instantiate a row with copying all cells from passed argument
    */
  def apply(cells: Cell[_]*): Row = apply(cells)

  /**
    * Instantiate a row with copying all cells from passed argument
    */
  def apply(cells: TraversableOnce[Cell[_]], _tags: Set[String] = Set.empty): Row = new Row() {

    /**
      * Save a array of cells in order to make size and index only require O(1) time
      */
    private[this] val cellArray = new ArrayBuffer[Cell[_]]
    private[this] val cellGroup = new mutable.LinkedHashMap[String, Cell[_]]()
    cells.foreach(
      (cell: Cell[_]) =>
        if (cellGroup.contains(cell.name)) throw new IllegalArgumentException(s"Duplicate name:${cell.name}")
        else {
          cellArray += cell
          if (cellGroup.put(cell.name, cell).isDefined)
            throw new IllegalArgumentException(s"duplicate column:${cell.name} are not supported")
      })

    override def cellCount: Int = cellGroup.size

    override def iterator: Iterator[Cell[_]] = cellGroup.valuesIterator

    override def seekCell(name: String): Option[Cell[_]] = cellGroup.get(name)

    override def names: Iterator[String] = cellGroup.keysIterator

    override def seekCell(index: Int): Cell[_] = try cellArray(index)
    catch {
      case e: ArrayIndexOutOfBoundsException => throw new IndexOutOfBoundsException(e.getMessage)
    }
    override def tags: Set[String] = _tags
  }
}
