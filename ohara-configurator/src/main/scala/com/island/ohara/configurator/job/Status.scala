package com.island.ohara.configurator.job

sealed abstract class Status {
  def name: String = getClass.getSimpleName
}
case object RUNNING extends Status
case object NON_RUNNING extends Status
case object NONEXIST extends Status

object Status {

  /**
    * @return a array of all supported data type
    */
  def all: Array[Status] = Array(RUNNING, NON_RUNNING, NONEXIST)

  /**
    * seek the data type by the type name
    * @param name index of data type
    * @return Data type
    */
  def of(name: String): Status = all.find(_.name.equalsIgnoreCase(name)).get
}
