package com.island.ohara.configurator.job

sealed abstract class Action {
  def name: String = getClass.getSimpleName
}
case object UPDATE extends Action
case object DELETE extends Action
case object RUN extends Action
case object PAUSE extends Action

object Action {

  /**
    * @return a array of all supported data type
    */
  def all: Array[Action] = Array(UPDATE, DELETE, RUN, PAUSE)

  /**
    * seek the data type by the type name
    * @param name index of data type
    * @return Data type
    */
  def of(name: String): Action = all.find(_.name.equalsIgnoreCase(name)).get
}
