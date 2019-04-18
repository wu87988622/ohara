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

package com.island.ohara.configurator.jar

import java.io.File
import java.net.URL

import com.island.ohara.client.configurator.v0.JarApi.JarInfo
import com.island.ohara.common.util.{CommonUtils, Releasable}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Plugin store used to keep the custom plugin (connector or streamapp) and provide a way to remote node to get the plugin.
  * In order to simplify the plugin store, the "way" used to get the plugin are format to URL. Also, the default implementation is based
  * on ftp, which can be load by java dynamical call.
  */
trait JarStore extends Releasable {

  /**
    * add a jar into store. This is a async method so you need to check the result of future.
    * @param file jar file
    * @return a async thread which is doing the upload
    */
  def add(file: File)(implicit executionContext: ExecutionContext): Future[JarInfo] =
    add(CommonUtils.requireExist(file), file.getName)

  /**
    * add a jar into store. This is a async method so you need to check the result of future.
    * @param file jar file
    * @param newName new name of jar file
    * @return a async thread which is doing the upload
    */
  def add(file: File, newName: String)(implicit executionContext: ExecutionContext): Future[JarInfo]

  /**
    * remove a existed jar file from jar store
    * @param id jar file's id
    */
  def remove(id: String)(implicit executionContext: ExecutionContext): Future[JarInfo]

  /**
    * update the jar
    * @param id jar's id
    * @param file new jar
    * @return a async thread which is doing the upload
    */
  def update(id: String, file: File)(implicit executionContext: ExecutionContext): Future[JarInfo]

  /**
    * retrieve the information of jar
    * @param id jar's id
    * @return jar description
    */
  def jarInfo(id: String)(implicit executionContext: ExecutionContext): Future[JarInfo] =
    jarInfos
      .map { jarInfos =>
        // check the input arguments
        CommonUtils.requireNonEmpty(id)
        jarInfos
      }
      .map(_.find(_.id == id).head)

  def jarInfos(implicit executionContext: ExecutionContext): Future[Seq[JarInfo]]

  /**
    * generate a downloadable remote resource.
    * @param id jar's id
    * @return url connection
    */
  def url(id: String)(implicit executionContext: ExecutionContext): Future[URL] =
    urls
      .map { us =>
        // check the input arguments
        CommonUtils.requireNonEmpty(id)
        us
      }
      .map(_.find(_._1 == id).head._2)

  /**
    * generate urls for all ids
    * @param executionContext thread pool
    * @return urls
    */
  def urls(implicit executionContext: ExecutionContext): Future[Map[String, URL]]

  /**
    * check the existence of a jar
    * @param id jar id
    * @param executionContext thread pool
    * @return true if jar exists. otherwise, false
    */
  def exist(id: String)(implicit executionContext: ExecutionContext): Future[Boolean]

  /**
    * Rename a existent jar
    * @param id jar id
    * @param newName new name of jar
    * @param executionContext thread pool
    * @return updated jar info
    */
  def rename(id: String, newName: String)(implicit executionContext: ExecutionContext): Future[JarInfo]

  /**
    * Create a local file storing the jar.
    * NOTED: you should NOT modify the file since the returned file may be referenced to true data.
    * @param id jar's id
    * @param executionContext thread pool
    * @return local file
    */
  def toFile(id: String)(implicit executionContext: ExecutionContext): Future[File]
}
