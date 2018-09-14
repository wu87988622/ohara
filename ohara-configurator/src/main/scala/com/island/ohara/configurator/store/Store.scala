package com.island.ohara.configurator.store

import com.island.ohara.serialization.Serializer

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

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
  def update(key: K, value: V, sync: Consistency): Future[Option[V]]

  /**
    * Retrieve the value by specified key.
    * @param key key mapped to the value
    * @return None if no key exist.
    */
  def get(key: K): Future[Option[V]]

  /**
    * Remove the value by specified key.
    * @param key key mapped to the value
    * @return previous value or None if no key exist.
    */
  def remove(key: K, sync: Consistency): Future[Option[V]]

  def exist(key: K): Future[Boolean]
}

/**
  * a helper interface to make all public methods in Store blocking
  * @tparam K key
  * @tparam V value
  */
trait BlockingStore[K, V] extends Store[K, V] {
  private[this] val timeout: Duration = 10 seconds
  def _get(key: K): Option[V] = Await.result(get(key), timeout)
  def _update(key: K, value: V, consistency: Consistency): Option[V] =
    Await.result(update(key, value, consistency), timeout)
  def _remove(key: K, consistency: Consistency): Option[V] = Await.result(remove(key, consistency), timeout)
  def _exist(key: K): Boolean = Await.result(exist(key), timeout)
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
  def inMemory[K, V](implicit keySerializer: Serializer[K], valueSerializer: Serializer[V]): BlockingStore[K, V] =
    new MemStore[K, V]

  def builder() = new StoreBuilder

  val DEFAULT_NUMBER_OF_REPLICATIONS: Short = 3
  val DEFAULT_NUMBER_OF_PARTITIONS: Int = 3
  val DEFAULT_INITIALIZATION_TIMEOUT: Duration = 10 seconds
  val DEFAULT_POLL_TIMEOUT: Duration = 5 seconds
  val DEFAULT_TAKE_TIMEOUT: Duration = 1 seconds
}

abstract sealed class Consistency
object Consistency {
  case object STRICT extends Consistency
  case object WEAK extends Consistency
  case object NONE extends Consistency
}
