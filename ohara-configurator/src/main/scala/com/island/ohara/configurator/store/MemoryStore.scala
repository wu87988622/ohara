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
import com.island.ohara.common.util.ByteUtils

import scala.concurrent.{ExecutionContext, Future}
private[store] class MemoryStore[K, V](keySerializer: Serializer[K], valueSerializer: Serializer[V])
    extends Store[K, V] {
  private[this] val store = new ConcurrentSkipListMap[Array[Byte], Array[Byte]](ByteUtils.BYTES_COMPARATOR)

  private[this] def toKey(key: K) = keySerializer.to(Objects.requireNonNull(key))
  private[this] def fromKey(key: Array[Byte]) = keySerializer.from(Objects.requireNonNull(key))
  private[this] def toValue(value: V) = valueSerializer.to(Objects.requireNonNull(value))
  private[this] def fromValue(value: Array[Byte]) = valueSerializer.from(Objects.requireNonNull(value))
  override def update(key: K, updater: V => Future[V])(implicit executor: ExecutionContext): Future[V] =
    value(key)
      .flatMap(updater)
      .map(newValue => {
        store.put(toKey(key), toValue(newValue))
        newValue
      })

  import scala.collection.JavaConverters._
  override def value(key: K)(implicit executor: ExecutionContext): Future[V] = Option(store.get(toKey(key)))
    .map(v => Future.successful(fromValue(v)))
    .getOrElse(Future.failed(
      new NoSuchElementException(s"$key doesn't exist. total:${store.keySet().asScala.map(fromKey).mkString(",")}")))
  override def values(keys: Seq[K])(implicit executor: ExecutionContext): Future[Map[K, V]] = {
    val r = keys.flatMap { key =>
      val value = store.get(key)
      if (value == null) None else Some(key -> fromValue(value))
    }.toMap
    val miss = keys.filterNot(k => r.keys.exists(_ == k))
    if (miss.isEmpty) Future.successful(r)
    else Future.failed(new NoSuchElementException(s"${miss.mkString(",")} don't exist!!!"))
  }

  import scala.collection.JavaConverters._
  override def values()(implicit executor: ExecutionContext): Future[Map[K, V]] = Future.successful(store.asScala.map {
    case (k, v) => fromKey(k) -> fromValue(v)
  }.toMap)

  override def close(): Unit = store.clear()

  override def remove(key: K)(implicit executor: ExecutionContext): Future[Boolean] =
    Future.successful(Option(store.remove(toKey(key))).nonEmpty)

  /**
    * Override the size to provide the efficient implementation
    * @return size from this store
    */
  override def size: Int = store.size()

  override def exist(key: K)(implicit executor: ExecutionContext): Future[Boolean] =
    Future.successful(store.containsKey(toKey(key)))

  override def add(key: K, value: V)(implicit executor: ExecutionContext): Future[V] = Future.successful {
    val previous = store.putIfAbsent(toKey(key), toValue(value))
    if (previous != null) throw new IllegalStateException(s"$key exists!!!")
    value
  }

  override def get(key: K)(implicit executor: ExecutionContext): Future[Option[V]] =
    Future.successful(Option(store.get(toKey(key))).map(fromValue))

}
