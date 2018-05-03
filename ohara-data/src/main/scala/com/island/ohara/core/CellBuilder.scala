package com.island.ohara.core

import CellBuilder._

/**
  * This builder enforce user must call the name before calling the build.
  * This builder support to build the cell with any type but not all type are supported to be serialized. Using the basic type is a
  * safe way to home.
  *
  * @tparam state a phantom type used to rule the call order
  */
abstract class CellBuilder[state <: State] {

  /**
    * set the name to the cell which will be built
    *
    * @param name cell name
    * @return the updated builder
    */
  def name(name: String): CellBuilder[state with Name]

  /**
    * Instantiate a cell
    *
    * @param v cell value
    * @return a cell with name and value
    */
  def build[T](v: T)(implicit state: state =:= FullState): Cell[T]
}

object CellBuilder {

  sealed trait State

  sealed trait Name extends State

  type FullState = Name
}
