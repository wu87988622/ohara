package com.island.ohara.client.configurator.v0

/**
  * This is the basic type which can be stored by configurator.
  * All members are declared as "def" since not all subclasses intend to represent all members in restful APIs.
  */
trait Data {
  def id: String
  def name: String
  def lastModified: Long
  def kind: String
}
