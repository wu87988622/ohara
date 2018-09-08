package com.island.ohara.data

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
  def cell(name: String): Cell[_]

  /**
    * seek the cell at the specific index
    *
    * @param index cell index
    * @return the cell at the specific index
    * @throws IndexOutOfBoundsException if `i < 0` or `size <= i`
    */
  def cell(index: Int): Cell[_]

  override def toString(): String = mkString(",")

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

  def equals(other: Row, includeTag: Boolean): Boolean =
    compareCell(other) && (!includeTag || compareTags(other))

  // TODO: evaluating the size first may be reduce the performance if the Row impl get the size by iterating. by chia
  private[this] def compareCell(other: Row): Boolean = if (isEmpty && other.isEmpty) true
  else if (size == other.size) forall(c => other.exists(_ == c))
  else false

  private[this] def compareTags(other: Row): Boolean = if (tags.isEmpty && other.tags.isEmpty) true
  else if (tags.size == other.tags.size) tags.forall(t => other.tags.contains(t))
  else false

  /**
    * the tag is useful to carry the extra meta for row
    * @return tags of this row
    */
  def tags: Set[String]
}

object Row {
  def builder(): RowBuilder = new RowBuilderImpl()

  /**
    * Instantiate a row with a single cell
    */
  def apply(cells: Cell[_]*): Row = builder().cells(cells).build()
}
