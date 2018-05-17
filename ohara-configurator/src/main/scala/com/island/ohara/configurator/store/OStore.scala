package com.island.ohara.configurator.store

import com.island.ohara.config.OharaConfig
import com.island.ohara.reflection.ReflectionUtil

/**
  * A key-value store. It is used to save the component information
  * NOTED: All implementation of OStore should be thread-safe.
  */
trait OStore[K, V] extends AutoCloseable with Iterable[(K, V)] {

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
}

object OStore {
  val COMPARATOR_IMPL = "ohara.ostore.comparator.impl"
  val OSTORE_IMPL = "ohara.ostore.impl"
  val OSTORE_IMPL_DEFAULT = classOf[MemOStore[_, _]].getName
  def apply[K, V](config: OharaConfig): OStore[K, V] =
    ReflectionUtil.instantiate(config.requireString(OSTORE_IMPL), classOf[OStore[K, V]], (classOf[OharaConfig], config))
  def apply[K, V](clzName: String): OStore[K, V] = ReflectionUtil.instantiate(clzName, classOf[OStore[K, V]])
}
