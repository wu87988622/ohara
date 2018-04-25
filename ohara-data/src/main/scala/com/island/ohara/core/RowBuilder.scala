package com.island.ohara.core

import scala.collection.TraversableOnce

abstract class RowBuilder {
  /**
    * Append a cell to the end of this builder
    *
    * @param cell the cell to add
    * @return the updated builder
    */
  def append(cell: Cell[_]): this.type = +=(cell)

  /**
    * Append a cell to the end of this builder
    *
    * @param cell the cell to add
    * @return the updated builder
    */
  def +=(cell: Cell[_]): this.type

  /**
    * Appends a number of cells to the builder
    *
    * @param cells the cells to add
    * @return the updated buffer.
    */
  def ++=(cells: TraversableOnce[Cell[_]]): this.type

  /**
    * Remove the specific cell from this builder
    *
    * @param cell the cell to remove
    * @return the updated builder
    */
  def remove(cell: Cell[_]): this.type = -=(cell)

  /**
    * Remove the specific cell from this builder
    *
    * @param cell the cell to remove
    * @return the updated builder
    */
  def -=(cell: Cell[_]): this.type

  /**
    * Replace the older by the newer. If the old one doesn't exist, the new one will be added to the end of this builder
    *
    * @param oldOne the cell will be removed
    * @param newOne the cell will be added
    * @return the updated builder
    */
  def replace(oldOne: Cell[_], newOne: Cell[_]): this.type

  /**
    * Removed all cells stored in this builder
    *
    * @return the updated builder
    */
  def clear(): this.type

  /**
    * Instantiate a row
    *
    * @return a row
    */
  def build(): Row
}