/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.configurator.store

import java.util.Objects
import java.util.concurrent.ConcurrentSkipListMap

import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.{ByteUtil, ReleaseOnce}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
  * A key-value store. It is used to save the component information
  * NOTED: All implementation from Store should be thread-safe.
  */
trait Store[K, V] extends ReleaseOnce {

  /**
    * add new key and value.
    * NOTED: Both key and value aren't nullable
    * @param key key
    * @param value value
    * @return the updated value
    */
  def add(key: K, value: V): Future[V]

  /**
    * Update the existent value with specified key.
    * NOTED: Both key and value aren't nullable
    * @param key key
    * @param value value
    * @return the updated value
    */
  def update(key: K, value: V => Future[V]): Future[V]

  /**
    * Retrieve the value by specified key.
    * @param key key mapped to the value
    */
  def value(key: K): Future[V]

  /**
    * Retrieve the values by specified keys.
    * NOTED: If any key in inputs doesn't exist in the store, an exception will be stored in Future.
    * @param keys keys mapped to the values
    */
  def values(keys: Seq[K]): Future[Map[K, V]]

  /**
    * Retrieve all values
    */
  def values(): Future[Map[K, V]]

  def size: Int

  /**
    * Remove the value by specified key.
    * @param key key mapped to the value
    * @return the removed value
    */
  def remove(key: K): Future[V]

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
    * @return An in-memory store
    */
  def inMemory[K, V](implicit keySerializer: Serializer[K], valueSerializer: Serializer[V]): Store[K, V] =
    new Store[K, V] {
      private[this] val store = new ConcurrentSkipListMap[Array[Byte], Array[Byte]](ByteUtil.BYTES_COMPARATOR)

      private[this] def toKey(key: K) = keySerializer.to(Objects.requireNonNull(key))
      private[this] def fromKey(key: Array[Byte]) = keySerializer.from(Objects.requireNonNull(key))
      private[this] def toValue(value: V) = valueSerializer.to(Objects.requireNonNull(value))
      private[this] def fromValue(value: Array[Byte]) = valueSerializer.from(Objects.requireNonNull(value))
      override def update(key: K, value: V => Future[V]): Future[V] = Future.successful(
        fromValue(
          store.computeIfPresent(
            toKey(key),
            // we have to block the method since the in-memory store we use doesn't support async get-and-update...by chia
            (_: Array[Byte], previous: Array[Byte]) => toValue(Await.result(value(fromValue(previous)), 30 seconds))
          )))

      override def value(key: K): Future[V] = Future.successful(
        Option(store.get(toKey(key))).map(fromValue).getOrElse(throw new NoSuchElementException(s"$key doesn't exist")))
      override def values(keys: Seq[K]): Future[Map[K, V]] =
        Future.successful(
          keys
            .map(key =>
              key -> fromValue(
                Option(store.get(toKey(key))).getOrElse(throw new NoSuchElementException(s"$key doesn't exist"))))
            .toMap)
      import scala.collection.JavaConverters._
      override def values(): Future[Map[K, V]] = Future.successful(store.asScala.map {
        case (k, v) => fromKey(k) -> fromValue(v)
      }.toMap)

      override protected def doClose(): Unit = store.clear()

      override def remove(key: K): Future[V] =
        Future.successful(Option(store.remove(toKey(key))).map(fromValue).get)

      /**
        * Override the size to provide the efficient implementation
        * @return size from this store
        */
      override def size: Int = store.size()

      override def exist(key: K): Future[Boolean] = Future.successful(store.containsKey(toKey(key)))

      override def add(key: K, value: V): Future[V] = Future.successful {
        val previous = store.putIfAbsent(toKey(key), toValue(value))
        if (previous != null) throw new IllegalStateException(s"$key exists!!!")
        value
      }
    }
}
