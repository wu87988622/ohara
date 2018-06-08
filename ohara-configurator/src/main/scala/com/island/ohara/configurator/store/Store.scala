package com.island.ohara.configurator.store

import com.island.ohara.serialization.Serializer

import scala.concurrent.duration._

/**
  * A key-value store. It is used to save the component information
  * NOTED: All implementation of Store should be thread-safe.
  */
trait Store[K, V] extends AutoCloseable with Iterable[(K, V)] {

  /**
    * Update the value with specified key. If the key-value exist, the new value will replace the previous value.
    * NOTED: Both key and value aren't nullable
    * @param key key
    * @param value value
    * @return Some[V] if the previous value exist
    */
  def update(key: K, value: V): Option[V]

  /**
    * Retrieve the value by specified key.
    * @param key key mapped to the value
    * @return None if no key exist.
    */
  def get(key: K): Option[V]

  /**
    * Remove the value by specified key.
    * @param key key mapped to the value
    * @return previous value or None if no key exist.
    */
  def remove(key: K): Option[V]

  /**
    * Removes and returns a key-value mapping associated with the least key in this map, or null if the map is empty.
    * @param timeout to poll
    * @return he removed first entry of this map, or null if this map is empty
    */
  def take(timeout: Duration = Store.DEFAULT_TAKE_TIMEOUT): Option[(K, V)]
}

object Store {
  def builder[K, V](keySerializer: Serializer[K], valueSerializer: Serializer[V]) =
    new StoreBuilder(keySerializer, valueSerializer)

  val DEFAULT_REPLICATION_NUMBER: Short = 3
  val DEFAULT_PARTITION_NUMBER: Int = 3
  val DEFAULT_INITIALIZATION_TIMEOUT: Duration = 10 seconds
  val DEFAULT_POLL_TIMEOUT: Duration = 5 seconds
  val DEFAULT_TAKE_TIMEOUT: Duration = 1 seconds
}
