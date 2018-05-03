package com.island.ohara.config

/**
  * A option of config.
  *
  * @tparam T the value converted from string config
  */
trait Property[T] {
  def key: String

  def default: T

  def description: String

  def from(value: String): T

  override def toString: String = s"$key:$default"
}

object Property {
  def builder = PropertyBuilder()
}
