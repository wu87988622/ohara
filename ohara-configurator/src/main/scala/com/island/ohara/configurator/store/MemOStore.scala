package com.island.ohara.configurator.store

import java.util.concurrent.ConcurrentSkipListMap
import java.util.{Comparator, Objects}

import com.island.ohara.config.OharaConfig
import com.island.ohara.io.CloseOnce
import com.island.ohara.reflection.ReflectionUtil

/**
  * All data are stored in memory. If user have set the comparator to properties, MemOStore will use the ConcurrentSkipListMap
  * to store the data.
  * NOTED: DON'T use MemOStore in your production since MemOStore doesn't support to persist data.
  * @param config config
  * @tparam K key
  * @tparam V value
  */
private class MemOStore[K, V](config: OharaConfig) extends OStore[K, V] with CloseOnce {
  private[this] val store =
    new ConcurrentSkipListMap[K, V](
      ReflectionUtil.instantiate(config.requireString(OStore.COMPARATOR_IMPL), classOf[Comparator[K]]))

  override def update(key: K, value: V): Option[V] = {
    checkClose()
    Option(store.put(Objects.requireNonNull(key), Objects.requireNonNull(value)))
  }

  override def get(key: K): Option[V] = {
    checkClose()
    Option(store.get(Objects.requireNonNull(key)))
  }

  override protected def doClose(): Unit = {
    store.clear()
  }

  override def remove(key: K): Option[V] = {
    checkClose()
    Option(store.remove(Objects.requireNonNull(key)))
  }

  override def iterator: Iterator[(K, V)] = {
    checkClose()
    return new Iterator[(K, V)]() {
      val iter = store.entrySet().iterator()
      override def hasNext: Boolean = iter.hasNext

      override def next(): (K, V) = {
        val entry = iter.next()
        (entry.getKey, entry.getValue)
      }
    }
  }

  /**
    * Overrride the size to provide the efficient implementation
    * @return size of this store
    */
  override def size: Int = store.size()
}
