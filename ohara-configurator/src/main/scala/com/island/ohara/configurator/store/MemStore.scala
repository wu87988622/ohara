package com.island.ohara.configurator.store

import java.util.Objects
import java.util.concurrent.ConcurrentSkipListMap

import com.island.ohara.io.{ByteUtil, CloseOnce}
import com.island.ohara.serialization.Serializer

import scala.concurrent.duration._

/**
  * All data are stored in memory. If user have set the comparator to properties, MemStore will use the ConcurrentSkipListMap
  * to store the data.
  * NOTED: DON'T use MemStore in your production since MemStore doesn't support to persist data.
  * @tparam K key
  * @tparam V value
  */
private class MemStore[K, V](implicit keySerializer: Serializer[K], valueSerializer: Serializer[V])
    extends Store[K, V]
    with CloseOnce {
  private[this] val store =
    new ConcurrentSkipListMap[Array[Byte], Array[Byte]](ByteUtil.COMPARATOR)

  private[this] def toKey(key: K) = keySerializer.to(Objects.requireNonNull(key))
  private[this] def fromKey(key: Array[Byte]) = keySerializer.from(Objects.requireNonNull(key))
  private[this] def toValue(value: V) = valueSerializer.to(Objects.requireNonNull(value))
  private[this] def fromValue(value: Array[Byte]) = valueSerializer.from(Objects.requireNonNull(value))
  private[this] val updateLock = new Object
  override def update(key: K, value: V): Option[V] = {
    try {
      checkClose()
      Option(store.put(toKey(key), toValue(value))).map(fromValue)
    } finally {
      updateLock.synchronized {
        updateLock.notifyAll()
      }
    }
  }

  override def get(key: K): Option[V] = {
    checkClose()
    Option(store.get(toKey(key))).map(fromValue)
  }

  override protected def doClose(): Unit = {
    store.clear()
    updateLock.synchronized {
      updateLock.notify()
    }
  }

  override def remove(key: K): Option[V] = {
    checkClose()
    Option(store.remove(toKey(key))).map(fromValue)
  }

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
    * @return size of this store
    */
  override def size: Int = store.size()

  /**
    * Removes and returns a key-value mapping associated with the least key in this map, or null if the map is empty.
    *
    * @return he removed first entry of this map, or null if this map is empty
    */
  override def take(timeout: Duration): Option[(K, V)] = {
    val end = timeout + (System.currentTimeMillis milliseconds)
    while (end.toMillis >= System.currentTimeMillis && !isClosed) {
      val first = store.pollFirstEntry()
      if (first != null) return Some((fromKey(first.getKey), fromValue(first.getValue)))
      updateLock.synchronized {
        updateLock.wait(timeout.toMillis)
      }
    }
    val first = store.pollFirstEntry()
    if (first != null) Some((fromKey(first.getKey), fromValue(first.getValue)))
    else None
  }

  override def clear(): Unit = store.clear()

  override def exist(key: K): Boolean = store.containsKey(toKey(key))
}
