package com.island.ohara.configurator.store

import java.util.Objects
import java.util.concurrent.ConcurrentSkipListMap

import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.{ByteUtil, CloseOnce}

import scala.concurrent.Future

/**
  * A key-value store. It is used to save the component information
  * NOTED: All implementation from Store should be thread-safe.
  */
trait Store[K, V] extends CloseOnce with Iterable[(K, V)] {

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

object Store {

  /**
    * All data are stored in memory. If user have set the comparator to properties, In-memory store will use the
    * ConcurrentSkipListMap to store the data.
    * NOTED: DON'T use in-memory store in your production since in-memory store doesn't support to persist data.
    * @param keySerializer key serializer
    * @param valueSerializer value serializer
    * @tparam K key type
    * @tparam V value type
    * @return a in-memory store
    */
  def inMemory[K, V](implicit keySerializer: Serializer[K], valueSerializer: Serializer[V]): Store[K, V] =
    new Store[K, V] {
      private[this] val store = new ConcurrentSkipListMap[Array[Byte], Array[Byte]](ByteUtil.BYTES_COMPARATOR)

      private[this] def toKey(key: K) = keySerializer.to(Objects.requireNonNull(key))
      private[this] def fromKey(key: Array[Byte]) = keySerializer.from(Objects.requireNonNull(key))
      private[this] def toValue(value: V) = valueSerializer.to(Objects.requireNonNull(value))
      private[this] def fromValue(value: Array[Byte]) = valueSerializer.from(Objects.requireNonNull(value))
      override def update(key: K, value: V, consistency: Consistency): Future[Option[V]] =
        Future.successful(Option(store.put(toKey(key), toValue(value))).map(fromValue))

      override def get(key: K): Future[Option[V]] = Future.successful(Option(store.get(toKey(key))).map(fromValue))

      override protected def doClose(): Unit = store.clear()

      override def remove(key: K, consistency: Consistency): Future[Option[V]] =
        Future.successful(Option(store.remove(toKey(key))).map(fromValue))

      override def iterator: Iterator[(K, V)] = new Iterator[(K, V)] {
        private[this] val iter = store.entrySet().iterator()
        override def hasNext: Boolean = iter.hasNext

        override def next(): (K, V) = {
          val entry = iter.next()
          (fromKey(entry.getKey), fromValue(entry.getValue))
        }
      }

      /**
        * Override the size to provide the efficient implementation
        * @return size from this store
        */
      override def size: Int = store.size()

      override def exist(key: K): Future[Boolean] = Future.successful(store.containsKey(toKey(key)))
    }

  def builder(): StoreBuilder = new StoreBuilder()
}

abstract sealed class Consistency
object Consistency {
  case object STRICT extends Consistency
  case object WEAK extends Consistency
  case object NONE extends Consistency
}
