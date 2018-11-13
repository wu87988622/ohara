package com.island.ohara.configurator.store

import java.util.Objects
import java.util.concurrent.ConcurrentSkipListMap

import com.island.ohara.client.util.CloseOnce
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.ByteUtil

import scala.concurrent.Future

/**
  * All data are stored in memory. If user have set the comparator to properties, MemStore will use the ConcurrentSkipListMap
  * to store the data.
  * NOTED: DON'T use MemStore in your production since MemStore doesn't support to persist data.
  * @tparam K key
  * @tparam V value
  */
private class MemStore[K, V](implicit keySerializer: Serializer[K], valueSerializer: Serializer[V])
    extends BlockingStore[K, V]
    with CloseOnce {
  private[this] val store =
    new ConcurrentSkipListMap[Array[Byte], Array[Byte]](ByteUtil.BYTES_COMPARATOR)

  private[this] def toKey(key: K) = keySerializer.to(Objects.requireNonNull(key))
  private[this] def fromKey(key: Array[Byte]) = keySerializer.from(Objects.requireNonNull(key))
  private[this] def toValue(value: V) = valueSerializer.to(Objects.requireNonNull(value))
  private[this] def fromValue(value: Array[Byte]) = valueSerializer.from(Objects.requireNonNull(value))
  override def update(key: K, value: V, consistency: Consistency): Future[Option[V]] =
    Future.successful(_update(key, value, consistency))

  override def get(key: K): Future[Option[V]] = Future.successful(_get(key))

  override protected def doClose(): Unit = {
    store.clear()
  }

  override def remove(key: K, consistency: Consistency): Future[Option[V]] =
    Future.successful(_remove(key, consistency))

  override def iterator: Iterator[(K, V)] = {
    checkClose()
    new Iterator[(K, V)]() {
      private[this] val iter = store.entrySet().iterator()
      override def hasNext: Boolean = iter.hasNext

      override def next(): (K, V) = {
        val entry = iter.next()
        (fromKey(entry.getKey), fromValue(entry.getValue))
      }
    }
  }

  /**
    * Overrride the size to provide the efficient implementation
    * @return size from this store
    */
  override def size: Int = store.size()

  override def exist(key: K): Future[Boolean] = Future.successful(_exist(key))

  //---------------------------------------[blocking method]---------------------------------------//
  // all methods in MemStore don't need to wait so we implement all blcking methods and then wrap them into Future-like.
  override def _get(key: K): Option[V] = {
    checkClose()
    Option(store.get(toKey(key))).map(fromValue)
  }
  override def _update(key: K, value: V, consistency: Consistency): Option[V] = {
    checkClose()
    Option(store.put(toKey(key), toValue(value))).map(fromValue)
  }
  override def _remove(key: K, consistency: Consistency): Option[V] = {
    checkClose()
    Option(store.remove(toKey(key))).map(fromValue)
  }
  override def _exist(key: K): Boolean = store.containsKey(toKey(key))
}
