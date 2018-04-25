package com.island.ohara.config

import PropertyBuilder._

class PropertyBuilder[state <: State] private {
  private[this] var key: String = _
  private[this] var description: String = _

  def key(key: String): PropertyBuilder[state with Key] = {
    this.key = key
    this.asInstanceOf[PropertyBuilder[state with Key]]
  }

  def description(description: String): PropertyBuilder[state with Desc] = {
    this.description = description
    this.asInstanceOf[PropertyBuilder[state with Desc]]
  }

  def build(default: String)(implicit state: state =:= FullState): Property[String] = new PropertyImpl(key, description, default) {
    override def from(value: String) = value
  }

  def build(default: Short)(implicit state: state =:= FullState): Property[Short] = new PropertyImpl(key, description, default) {
    override def from(value: String) = value.toShort
  }

  def build(default: Int)(implicit state: state =:= FullState): Property[Int] = new PropertyImpl(key, description, default) {
    override def from(value: String) = value.toInt
  }

  def build(default: Long)(implicit state: state =:= FullState): Property[Long] = new PropertyImpl(key, description, default) {
    override def from(value: String) = value.toLong
  }

  def build(default: Float)(implicit state: state =:= FullState): Property[Float] = new PropertyImpl(key, description, default) {
    override def from(value: String) = value.toFloat
  }

  def build(default: Double)(implicit state: state =:= FullState): Property[Double] = new PropertyImpl(key, description, default) {
    override def from(value: String) = value.toDouble
  }

  def build(default: Boolean)(implicit state: state =:= FullState): Property[Boolean] = new PropertyImpl(key, description, default) {
    override def from(value: String) = value.toBoolean
  }
}

object PropertyBuilder {
  private[config] def apply() = new PropertyBuilder[State]()

  private abstract class PropertyImpl[T](val key: String, val description: String, val default: T) extends Property[T]

  sealed trait State

  sealed trait Key extends State

  sealed trait Desc extends State

  type FullState = Key with Desc
}
