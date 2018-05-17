package com.island.ohara.configurator.serialization

/**
  * Used to convert a T object to V
  *
  * @tparam T object type
  * @tparam V a serializable type
  */
trait Serializer[T, V] {

  /**
    * Convert the object to a serializable type
    * @param obj object
    * @return a serializable type
    */
  def to(obj: T): V

  /**
    * Convert the serialized data to object
    * @param serial serialized data
    * @return object
    */
  def from(serial: V): T
}
