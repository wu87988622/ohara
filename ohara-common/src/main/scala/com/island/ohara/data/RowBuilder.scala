package com.island.ohara.data

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
    * @param cells the cells to add
    * @return the updated builder
    */
  def cells(cells: Seq[Cell[_]]): RowBuilder

  /**
    * Instantiate a row
    *
    * @return a row
    */
  def build(): Row
}
