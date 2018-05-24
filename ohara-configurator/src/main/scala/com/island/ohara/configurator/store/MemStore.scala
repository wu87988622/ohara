package com.island.ohara.configurator.store

import java.util.Objects
import java.util.concurrent.ConcurrentSkipListMap

import com.island.ohara.config.OharaConfig
import com.island.ohara.io.{ByteUtil, CloseOnce}

/**
  * All data are stored in memory. If user have set the comparator to properties, MemStore will use the ConcurrentSkipListMap
  * to store the data.
  * NOTED: DON'T use MemStore in your production since MemStore doesn't support to persist data.
  * @param config config
  * @tparam K key
  * @tparam V value
  */
private class MemStore[K, V](config: OharaConfig) extends Store[K, V](config) with CloseOnce {
  private[this] val store =
    new ConcurrentSkipListMap[Array[Byte], Array[Byte]](ByteUtil.COMPARATOR)

  private[this] def toKey(key: K) = keySerializer.to(Objects.requireNonNull(key))
  private[this] def fromKey(key: Array[Byte]) = keySerializer.from(Objects.requireNonNull(key))
  private[this] def toValue(value: V) = valueSerializer.to(Objects.requireNonNull(value))
  private[this] def fromValue(value: Array[Byte]) = valueSerializer.from(Objects.requireNonNull(value))

  override def update(key: K, value: V): Option[V] = {
    checkClose()
    Option(store.put(toKey(key), toValue(value))).map(fromValue(_))
  }

  override def get(key: K): Option[V] = {
    checkClose()
    Option(store.get(toKey(key))).map(fromValue(_))
  }

  override protected def doClose(): Unit = {
    store.clear()
  }

  override def remove(key: K): Option[V] = {
    checkClose()
    Option(store.remove(toKey(key))).map(fromValue(_))
  }

  override def iterator: Iterator[(K, V)] = {
    checkClose()
    return new Iterator[(K, V)]() {
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
}
