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

import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.{CommonUtils, Releasable}

import scala.concurrent.{ExecutionContext, Future}

/**
  * A key-value store. It is used to save the component information
  * NOTED: All implementation from Store should be thread-safe.
  */
trait Store[K, V] extends Releasable {

  /**
    * add new key and value.
    * NOTED: Both key and value aren't nullable
    * @param key key
    * @param value value
    * @return the updated value
    */
  def add(key: K, value: V)(implicit executor: ExecutionContext): Future[V]

  /**
    * Update the existent value with specified key.
    * NOTED: Both key and value aren't nullable
    * @param key key
    * @param updater updater
    * @return the updated value
    */
  def update(key: K, updater: V => Future[V])(implicit executor: ExecutionContext): Future[V]

  /**
    * return the value associated to key or empty if key doesn't exist
    * @param key key
    * @return value
    */
  def get(key: K)(implicit executor: ExecutionContext): Future[Option[V]]

  /**
    * Retrieve the value by specified key.
    * @param key key mapped to the value
    */
  def value(key: K)(implicit executor: ExecutionContext): Future[V]

  /**
    * Retrieve the values by specified keys.
    * NOTED: If any key in inputs doesn't exist in the store, an exception will be stored in Future.
    * @param keys keys mapped to the values
    */
  def values(keys: Seq[K])(implicit executor: ExecutionContext): Future[Map[K, V]]

  /**
    * Retrieve all values
    */
  def values()(implicit executor: ExecutionContext): Future[Map[K, V]]

  def size: Int

  /**
    * Remove the value by specified key.
    * @param key key mapped to the value
    * @return the removed value
    */
  def remove(key: K)(implicit executor: ExecutionContext): Future[Boolean]

  def exist(key: K)(implicit executor: ExecutionContext): Future[Boolean]
}

object Store {

  def builder[K, V](): Builder[K, V] = new Builder[K, V]

  class Builder[K, V] private[Store] {
    private[this] var keySerializer: Serializer[K] = _
    private[this] var valueSerializer: Serializer[V] = _
    private[this] var _inMemory = true
    private[this] var persistentFolder: String = _

    def keySerializer(keySerializer: Serializer[K]): Builder[K, V] = {
      this.keySerializer = Objects.requireNonNull(keySerializer)
      this
    }

    def valueSerializer(valueSerializer: Serializer[V]): Builder[K, V] = {
      this.valueSerializer = Objects.requireNonNull(valueSerializer)
      this
    }

    @Optional("Default value is empty")
    def persistentFolder(persistentFolder: String): Builder[K, V] = {
      this.persistentFolder = CommonUtils.requireNonEmpty(persistentFolder)
      this._inMemory = false
      this
    }

    /**
      * All data are stored in memory. If user have set the comparator to properties, In-memory store will use the
      * ConcurrentSkipListMap to store the data.
      * NOTED: DON'T use in-memory store in your production since in-memory store doesn't support to persist data.
      */
    @Optional("Default value is true")
    def inMemory(): Builder[K, V] = {
      this.persistentFolder = null
      this._inMemory = true
      this
    }

    def build(): Store[K, V] = {
      Objects.requireNonNull(keySerializer)
      Objects.requireNonNull(valueSerializer)
      if (_inMemory) new MemoryStore(keySerializer, valueSerializer)
      else new RocksStore(CommonUtils.requireNonEmpty(persistentFolder), keySerializer, valueSerializer)
    }
  }
}
