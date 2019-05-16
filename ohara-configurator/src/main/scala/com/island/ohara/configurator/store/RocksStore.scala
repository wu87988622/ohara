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

import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.Releasable
import org.rocksdb.{Options, RocksDB}

import scala.concurrent.{ExecutionContext, Future}

private[store] class RocksStore[K, V](folder: String, keySerializer: Serializer[K], valueSerializer: Serializer[V])
    extends Store[K, V] {

  private[this] val options = {
    RocksDB.loadLibrary()
    new Options().setCreateIfMissing(true)
  }

  private[this] val db = {
    try RocksDB.open(options, folder)
    catch {
      case e: Throwable =>
        Releasable.close(options)
        throw e
    }
  }

  private[this] def toKey(key: K) = keySerializer.to(Objects.requireNonNull(key))
  private[this] def fromKey(key: Array[Byte]) = keySerializer.from(Objects.requireNonNull(key))
  private[this] def toValue(value: V) = valueSerializer.to(Objects.requireNonNull(value))
  private[this] def fromValue(value: Array[Byte]) = valueSerializer.from(Objects.requireNonNull(value))

  override def add(key: K, value: V)(implicit executor: ExecutionContext): Future[V] = exist(key).map {
    if (_) throw new IllegalStateException(s"$key already exists")
    else {
      db.put(toKey(key), toValue(value))
      value
    }
  }

  override def update(key: K, updater: V => Future[V])(implicit executor: ExecutionContext): Future[V] =
    value(key)
      .flatMap(updater)
      .map(newValue => {
        db.put(toKey(key), toValue(newValue))
        newValue
      })

  override def get(key: K)(implicit executor: ExecutionContext): Future[Option[V]] =
    Future.successful(Option(db.get(toKey(key))).map(fromValue))

  override def value(key: K)(implicit executor: ExecutionContext): Future[V] =
    Option(db.get(toKey(key)))
      .map(v => Future.successful(fromValue(v)))
      .getOrElse(Future.failed(new NoSuchElementException(s"$key doesn't exist")))

  override def values(keys: Seq[K])(implicit executor: ExecutionContext): Future[Map[K, V]] = {
    val r = keys.flatMap { key =>
      val value = db.get(toKey(key))
      if (value == null) None else Some(key -> fromValue(value))
    }.toMap
    val miss = keys.filterNot(k => r.keys.exists(_ == k))
    if (miss.isEmpty) Future.successful(r)
    else Future.failed(new NoSuchElementException(s"${miss.mkString(",")} don't exist!!!"))
  }

  override def values()(implicit executor: ExecutionContext): Future[Map[K, V]] = {
    val iter = db.newIterator()
    iter.seekToFirst()
    val data = try Iterator
      .continually(
        if (iter.isValid)
          try Some((fromKey(iter.key()), fromValue(iter.value())))
          finally iter.next()
        else None)
      .takeWhile(_.isDefined)
      .flatten
      .toList
      .toMap
    finally iter.close()
    Future.successful(data)
  }

  override def size: Int = {
    // TODO: is there a counter for rocksdb ???  by chia
    val iter = db.newIterator()
    iter.seekToFirst()
    try {
      var count = 0
      while (iter.isValid) {
        count = count + 1
        iter.next()
      }
      count
    } finally iter.close()
  }

  override def remove(key: K)(implicit executor: ExecutionContext): Future[Boolean] = exist(key).map {
    if (_) {
      db.delete(toKey(key))
      true
    } else false
  }

  override def exist(key: K)(implicit executor: ExecutionContext): Future[Boolean] =
    Future.successful(db.get(toKey(key)) != null)

  override def close(): Unit = {
    Releasable.close(db)
    Releasable.close(options)
  }
}
