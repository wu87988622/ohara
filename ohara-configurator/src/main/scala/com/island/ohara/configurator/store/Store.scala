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

  /**
    * remove all data from this store.
    * NOTED: this operation is expensive when the store is based on kafka topic.
    */
  def clear(): Unit
}

object Store {

  /**
    * create a store based on memory. This store does not require the kafka cluster.
    * @param keySerializer key serializer
    * @param valueSerializer value serializer
    * @tparam K key type
    * @tparam V value type
    * @return a in-memory store
    */
  def inMemory[K, V](implicit keySerializer: Serializer[K], valueSerializer: Serializer[V]): Store[K, V] =
    new MemStore[K, V]

  def builder() = new StoreBuilder

  val DEFAULT_NUMBER_OF_REPLICATIONS: Short = 3
  val DEFAULT_NUMBER_OF_PARTITIONS: Int = 3
  val DEFAULT_INITIALIZATION_TIMEOUT: Duration = 10 seconds
  val DEFAULT_POLL_TIMEOUT: Duration = 5 seconds
  val DEFAULT_TAKE_TIMEOUT: Duration = 1 seconds
}
