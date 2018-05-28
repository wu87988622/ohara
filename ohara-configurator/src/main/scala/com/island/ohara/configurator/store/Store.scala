package com.island.ohara.configurator.store

import com.island.ohara.config.{OharaConfig, Property}
import com.island.ohara.reflection.ReflectionUtil
import com.island.ohara.serialization.Serializer

/**
  * A key-value store. It is used to save the component information
  * NOTED: All implementation of Store should be thread-safe.
  */
abstract class Store[K, V](config: OharaConfig) extends AutoCloseable with Iterable[(K, V)] {

  /**
    * instantiate the key serializer used to do the conversion between key object and byte array.
    */
  protected val keySerializer: Serializer[K] =
    ReflectionUtil.instantiate(Store.KEY_SERIALIZER_IMPL.require(config), classOf[Serializer[K]])

  /**
    * instantiate the key serializer used to do the conversion between value object and byte array.
    */
  protected val valueSerializer: Serializer[V] =
    ReflectionUtil.instantiate(Store.VALUE_SERIALIZER_IMPL.require(config), classOf[Serializer[V]])

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

  val KEY_SERIALIZER_IMPL: Property[String] = Property.builder
    .key("ohara.store.key.serializer.impl")
    .description("the full class name of key serializer implementation")
    .stringProperty

  val VALUE_SERIALIZER_IMPL: Property[String] = Property.builder
    .key("ohara.store.value.serializer.impl")
    .description("the full class name of value serializer implementation")
    .stringProperty

  val STORE_IMPL: Property[String] = Property.builder
    .key("ohara.store.impl")
    .description("the full class name of store implementation")
    .stringProperty(classOf[TopicStore[_, _]].getName)

  def apply[K, V](config: OharaConfig): Store[K, V] =
    ReflectionUtil.instantiate(STORE_IMPL.require(config), classOf[Store[K, V]], (classOf[OharaConfig], config))

  def apply[K, V](clzName: String): Store[K, V] = ReflectionUtil.instantiate(clzName, classOf[Store[K, V]])
}
