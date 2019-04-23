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

import com.island.ohara.client.configurator.v0.Data
import com.island.ohara.common.util.{Releasable, ReleaseOnce}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}

trait DataStore extends Releasable {
  def value[T <: Data: ClassTag](id: String)(implicit executor: ExecutionContext): Future[T]

  def values[T <: Data: ClassTag](implicit executor: ExecutionContext): Future[Seq[T]]

  def raw()(implicit executor: ExecutionContext): Future[Seq[Data]]

  def raw(id: String)(implicit executor: ExecutionContext): Future[Data]

  /**
    * Remove a "specified" sublcass from ohara data mapping the id. If the data mapping to the id is not the specified
    * type, an exception will be thrown.
    *
    * @param id from ohara data
    * @tparam T subclass type
    * @return the removed data
    */
  def remove[T <: Data: ClassTag](id: String)(implicit executor: ExecutionContext): Future[T]

  /**
    * update an existed object in the store. If the id doesn't  exists, an exception will be thrown.
    *
    * @param data data
    * @tparam T type from data
    * @return the removed data
    */
  def update[T <: Data: ClassTag](id: String, data: T => Future[T])(implicit executor: ExecutionContext): Future[T]

  def add[T <: Data](data: T)(implicit executor: ExecutionContext): Future[T]

  def exist[T <: Data: ClassTag](id: String)(implicit executor: ExecutionContext): Future[Boolean]

  def nonExist[T <: Data: ClassTag](id: String)(implicit executor: ExecutionContext): Future[Boolean]

  def size: Int
}

object DataStore {
  def apply(store: Store[String, Data]): DataStore = new DataStoreImpl(store)

  private[this] class DataStoreImpl(store: Store[String, Data]) extends ReleaseOnce with DataStore {

    override protected def doClose(): Unit = store.close()

    override def value[T <: Data: ClassTag](id: String)(implicit executor: ExecutionContext): Future[T] =
      store.value(id).filter(classTag[T].runtimeClass.isInstance).map(_.asInstanceOf[T])

    override def values[T <: Data: ClassTag](implicit executor: ExecutionContext): Future[Seq[T]] =
      store.values().map(_.values.filter(classTag[T].runtimeClass.isInstance).map(_.asInstanceOf[T]).toSeq)

    override def raw()(implicit executor: ExecutionContext): Future[Seq[Data]] = store.values().map(_.values.toSeq)

    override def raw(id: String)(implicit executor: ExecutionContext): Future[Data] = store.value(id)

    /**
      * Remove a "specified" sublcass from ohara data mapping the id. If the data mapping to the id is not the specified
      * type, an exception will be thrown.
      *
      * @param id from ohara data
      * @tparam T subclass type
      * @return the removed data
      */
    override def remove[T <: Data: ClassTag](id: String)(implicit executor: ExecutionContext): Future[T] =
      value[T](id).flatMap(_ => store.remove(id)).map(_.asInstanceOf[T])

    /**
      * update an existed object in the store. If the id doesn't  exists, an exception will be thrown.
      *
      * @param data data
      * @tparam T type from data
      * @return the removed data
      */
    override def update[T <: Data: ClassTag](id: String, data: T => Future[T])(
      implicit executor: ExecutionContext): Future[T] =
      value[T](id).flatMap(_ => store.update(id, v => data(v.asInstanceOf[T]))).map(_.asInstanceOf[T])

    override def add[T <: Data](data: T)(implicit executor: ExecutionContext): Future[T] =
      store.add(data.id, data).map(_.asInstanceOf[T])

    override def exist[T <: Data: ClassTag](id: String)(implicit executor: ExecutionContext): Future[Boolean] =
      store.exist(id).flatMap { isExist =>
        if (isExist)
          store.value(id).map(classTag[T].runtimeClass.isInstance)
        else
          Future.successful(false)
      }

    override def nonExist[T <: Data: ClassTag](id: String)(implicit executor: ExecutionContext): Future[Boolean] =
      exist[T](id).map(!_)

    override def size: Int = store.size
  }

}
