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

import java.util
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap

import com.island.ohara.client.configurator.v0.Data
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.Releasable
import org.rocksdb.{ColumnFamilyDescriptor, _}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}

/**
  * RocksStore is based on Facebook RocksDB. The different type is stored in different column family.
  * @param folder used to store data in disk
  * @param keySerializer key serializer
  * @param valueSerializer value serializer
  */
private[store] class RocksDataStore(folder: String,
                                    keySerializer: Serializer[String],
                                    valueSerializer: Serializer[Data])
    extends DataStore {

  private[this] val handlers = new ConcurrentHashMap[String, ColumnFamilyHandle]()

  private[this] val db = {
    RocksDB.loadLibrary()
    val cfs = {
      val options = new Options().setCreateIfMissing(true)
      try RocksDB.listColumnFamilies(options, folder)
      finally options.close()
    }.asScala
    val lists = new util.ArrayList[ColumnFamilyHandle]()
    val options = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)
    try RocksDB.open(
      options,
      folder,
      (cfs.map(name => new ColumnFamilyDescriptor(name, new ColumnFamilyOptions))
      // RocksDB demands us to define Default column family
        ++ Seq(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, new ColumnFamilyOptions))).asJava,
      lists
    )
    finally {
      options.close()
      lists.asScala.foreach(handler => handlers.put(new String(handler.getName), handler))
    }
  }

  private[this] def getOrCreateHandler[T <: Data: ClassTag]: ColumnFamilyHandle = getOrCreateHandler(
    classTag[T].runtimeClass)

  private[this] def getOrCreateHandler(clz: Class[_]): ColumnFamilyHandle =
    handlers.computeIfAbsent(clz.getName, name => db.createColumnFamily(new ColumnFamilyDescriptor(name.getBytes)))

  private[this] def toMap(iter: RocksIterator, firstKey: String, endKey: String): Map[String, Data] =
    try {
      if (firstKey == null) iter.seekToFirst() else iter.seek(toKey(firstKey))
      Iterator
        .continually(
          if (iter.isValid)
            try Some((fromKey(iter.key()), fromValue(iter.value())))
            finally iter.next()
          else None)
        .takeWhile(_.exists {
          case (k, _) => endKey == null || k == endKey
        })
        .flatten
        .toList
        .toMap
    } finally iter.close()

  private[this] def toKey(key: String): Array[Byte] = keySerializer.to(Objects.requireNonNull(key))
  private[this] def fromKey(key: Array[Byte]): String = keySerializer.from(Objects.requireNonNull(key))
  private[this] def toValue(value: Data): Array[Byte] = valueSerializer.to(Objects.requireNonNull(value))
  private[this] def fromValue(value: Array[Byte]): Data = valueSerializer.from(Objects.requireNonNull(value))

  private[this] def _get(handler: ColumnFamilyHandle, name: String): Option[Data] =
    Option(db.get(handler, toKey(name))).map(fromValue)

  override def get[T <: Data: ClassTag](name: String)(implicit executor: ExecutionContext): Future[Option[T]] =
    Future.successful(_get(getOrCreateHandler[T], name).map(_.asInstanceOf[T]))

  override def value[T <: Data: ClassTag](name: String)(implicit executor: ExecutionContext): Future[T] = get[T](name)
    .map(_.getOrElse(throw new NoSuchElementException(s"$name doesn't exist in ${classTag[T].runtimeClass.getName}")))

  override def values[T <: Data: ClassTag]()(implicit executor: ExecutionContext): Future[Seq[T]] =
    Future.successful(
      toMap(db.newIterator(getOrCreateHandler[T]), null, null)
        .map {
          case (k, v) => k -> v.asInstanceOf[T]
        }
        .values
        .toSeq)

  override def remove[T <: Data: ClassTag](name: String)(implicit executor: ExecutionContext): Future[Boolean] =
    get[T](name).map { obj =>
      if (obj.isDefined) db.delete(getOrCreateHandler[T], toKey(name))
      obj.isDefined
    }

  override def addIfPresent[T <: Data: ClassTag](name: String, updater: T => Future[T])(
    implicit executor: ExecutionContext): Future[T] =
    value[T](name)
      .flatMap(updater)
      .map(newValue => {
        db.put(getOrCreateHandler(newValue.getClass), toKey(newValue.id), toValue(newValue))
        newValue
      })

  override def addIfAbsent[T <: Data](key: String, data: T)(implicit executor: ExecutionContext): Future[T] =
    Future.successful {
      if (_get(getOrCreateHandler(data.getClass), data.id).isDefined)
        throw new IllegalStateException(s"$key exists on ${data.getClass.getName}")
      else {
        db.put(getOrCreateHandler(data.getClass), toKey(key), toValue(data))
        data
      }
    }

  override def add[T <: Data](key: String, data: T)(implicit executor: ExecutionContext): Future[T] =
    Future.successful {
      db.put(getOrCreateHandler(data.getClass), toKey(key), toValue(data))
      data
    }

  override def exist[T <: Data: ClassTag](name: String)(implicit executor: ExecutionContext): Future[Boolean] =
    get[T](name).map(_.isDefined)

  override def nonExist[T <: Data: ClassTag](name: String)(implicit executor: ExecutionContext): Future[Boolean] =
    get[T](name).map(_.isEmpty)

  override def size(): Int =
    handlers
      .values()
      .asScala
      .toSeq
      .map { handler =>
        // TODO: is there a counter for rocksdb ???  by chia
        val iter = db.newIterator(handler)
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
      .sum

  override def close(): Unit = {
    handlers.values().forEach(h => Releasable.close(h))
    Releasable.close(db)
  }

  override def raws()(implicit executor: ExecutionContext): Future[Seq[Data]] =
    Future.successful(
      handlers.values().asScala.toSeq.flatMap(handler => toMap(db.newIterator(handler), null, null).values.toSeq))

  override def raws(name: String)(implicit executor: ExecutionContext): Future[Seq[Data]] =
    Future.successful(
      handlers.values().asScala.toSeq.flatMap(handler => toMap(db.newIterator(handler), name, name).values.toSeq))

  /**
    * RocksDB has a default cf in creating, and the cf is useless to us so we should not count it.
    * @return number of stored data types.
    */
  override def numberOfTypes(): Int = handlers.size() - 1
}
