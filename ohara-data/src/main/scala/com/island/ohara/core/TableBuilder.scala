package com.island.ohara.core

import scala.collection.TraversableOnce

abstract class TableBuilder {
  /**
    * Append a row to the end of this builder
    *
    * @param row the row to add
    * @return the updated builder
    */
  def +=(row: Row): this.type

  /**
    * Append a row to the end of this builder
    *
    * @param row the row to add
    * @return the updated builder
    */
  def append(row: Row): this.type = +=(row)

  /**
    * Appends a number of rows to the builder
    *
    * @param rows the rows to add
    * @return the updated buffer.
    */
  def ++=(rows: TraversableOnce[Row]): this.type

  /**
    * Remove the specific row from this builder
    *
    * @param row the row to remove
    * @return the updated builder
    */
  def remove(row: Row): this.type = -=(row)

  /**
    * Remove the specific row from this builder
    *
    * @param row the row to remove
    * @return the updated builder
    */
  def -=(row: Row): this.type

  /**
    * Replace the older by the newer. If the old one doesn't exist, the new one will be added to the end of this builder
    *
    * @param oldOne the row will be removed
    * @param newOne the row will be added
    * @return the updated builder
    */
  def replace(oldOne: Row, newOne: Row): this.type

  /**
    * Removed all rows stored in this builder
    *
    * @return the updated builder
    */
  def clear(): this.type

  /**
    * Instantiate a table
    *
    * @return a table
    */
  def build(): Table
}