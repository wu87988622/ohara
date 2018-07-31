package com.island.ohara.data

/**
  * This builder enforce user must call the name before calling the build.
  * This builder support to build the cell with any type but not all type are supported to be serialized. Using the basic type is a
  * safe way to home.
  *
  */
abstract class CellBuilder {

  /**
    * set the name to the cell which will be built
    *
    * @param name cell name
    * @return the updated builder
    */
  def name(name: String): CellBuilder

  /**
    * Instantiate a cell
    *
    * @param v cell value
    * @return a cell with name and value
    */
  def build[T](v: T): Cell[T]
}
