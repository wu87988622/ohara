package com.island.ohara.configurator.store

import com.island.ohara.config.OharaConfig
import com.island.ohara.configurator.serialization.Serializer
import com.island.ohara.reflection.ReflectionUtil

/**
  * A key-value store. It is used to save the component information
  * NOTED: All implementation of Store should be thread-safe.
  */
abstract class Store[K, V](config: OharaConfig) extends AutoCloseable with Iterable[(K, V)] {

  /**
    * instantiate the key serializer used to do the conversion between key object and byte array.
    */
  protected val keySerializer: Serializer[K, Array[Byte]] =
    ReflectionUtil.instantiate(config.requireString(Store.KEY_SERIALIZER_IMPL), classOf[Serializer[K, Array[Byte]]])

  /**
    * instantiate the key serializer used to do the conversion between value object and byte array.
    */
  protected val valueSerializer: Serializer[V, Array[Byte]] =
    ReflectionUtil.instantiate(config.requireString(Store.VALUE_SERIALIZER_IMPL), classOf[Serializer[V, Array[Byte]]])

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

object Store {
  val KEY_SERIALIZER_IMPL = "ohara.store.key.serializer.impl"
  val VALUE_SERIALIZER_IMPL = "ohara.store.value.serializer.impl"
  val STORE_IMPL = "ohara.store.impl"
  val STORE_IMPL_DEFAULT = classOf[MemStore[_, _]].getName
  def apply[K, V](config: OharaConfig): Store[K, V] =
    ReflectionUtil.instantiate(config.requireString(STORE_IMPL), classOf[Store[K, V]], (classOf[OharaConfig], config))
  def apply[K, V](clzName: String): Store[K, V] = ReflectionUtil.instantiate(clzName, classOf[Store[K, V]])
}
