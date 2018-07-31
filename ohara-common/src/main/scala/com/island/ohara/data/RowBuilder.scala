package com.island.ohara.data

import scala.collection.TraversableOnce
import scala.collection.immutable.Set

abstract class RowBuilder {

  /**
    * set the tags.
    * @param tags tags
    * @return this builder
    */
  def tags(tags: Set[String]): RowBuilder

  /**
    * Append a cell to the end of this builder
    *
    * @param cell the cell to add
    * @return the updated builder
    */
  def append(cell: Cell[_]): RowBuilder

  /**
    * Append a cell to the end of this builder
    *
    * @param cell the cell to add
    * @return the updated builder
    */
  def append(cells: TraversableOnce[Cell[_]]): RowBuilder

  /**
    * Remove the specific cell from this builder
    *
    * @param cell the cell to remove
    * @return the updated builder
    */
  def remove(cell: Cell[_]): RowBuilder

  /**
    * Replace the older by the newer. If the old one doesn't exist, the new one will be added to the end of this builder
    *
    * @param oldOne the cell will be removed
    * @param newOne the cell will be added
    * @return the updated builder
    */
  def replace(oldOne: Cell[_], newOne: Cell[_]): RowBuilder

  /**
    * Removed all cells stored in this builder
    *
    * @return the updated builder
    */
  def clear(): RowBuilder

  /**
    * Instantiate a row
    *
    * @return a row
    */
  def build(): Row
}
