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

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.island.ohara.client.configurator.ConfiguratorApiInfo
import com.island.ohara.client.configurator.v0.JarApi.JarInfo
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Plugin store used to keep the custom plugin (connector or streamApp) and provide a way to remote node to get the plugin.
  * In order to simplify the plugin store, the "way" used to get the plugin are format to URL. Also, the default implementation is based
  * on ftp, which can be load by java dynamical call.
  */
trait JarStore extends Releasable {

  /**
    * add a jar into store. This is an async method so you need to check the result of future.
    * @param file jar file
    * @param group group name. default is random string
    * @return a async thread which is doing the upload
    */
  def add(file: File, group: Option[String])(implicit executionContext: ExecutionContext): Future[JarInfo] =
    add(CommonUtils.requireExist(file), file.getName, group)

  /**
    * add a jar into store with specified file name. This is an async method so you need to check the result of future.
    * @param file jar file
    * @param newName new name of jar file
    * @param group group name. default is random string
    * @return a async thread which is doing the upload
    */
  def add(file: File, newName: String, group: Option[String])(
    implicit executionContext: ExecutionContext): Future[JarInfo]

  /**
    * remove a existed jar file from jar store
    * @param id jar file's id
    */
  def remove(id: String)(implicit executionContext: ExecutionContext): Future[Boolean]

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
  def jarInfo(id: String)(implicit executionContext: ExecutionContext): Future[JarInfo] = {
    CommonUtils.requireNonEmpty(id)
    jarInfos.map(infos => {
      infos.find(_.id == id).getOrElse(throw new NoSuchElementException(s"$id not found"))
    })
  }

  def jarInfos()(implicit executionContext: ExecutionContext): Future[Seq[JarInfo]]

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

  /**
    * generate the route offering the file download of this jar store
    * @param executionContext thread pool
    * @return server route
    */
  def route(implicit executionContext: ExecutionContext): server.Route
}

object JarStore {

  def builder: Builder = new Builder

  class Builder private[jar] {
    private[this] var homeFolder: String = _
    private[this] var hostname: String = _
    private[this] var port: Int = -1

    def homeFolder(homeFolder: String): Builder = {
      this.homeFolder = CommonUtils.requireNonEmpty(homeFolder)
      this
    }

    /**
      * set the hostname added to the URL. Noted that jar store won't instantiate a server internally
      * @param hostname hostname of url
      * @return this builder
      */
    def hostname(hostname: String): Builder = {
      this.hostname = CommonUtils.requireNonEmpty(hostname)
      this
    }

    /**
      * set the port added to the URL. Noted that jar store won't instantiate a server internally
      * @param port port of url
      * @return this builder
      */
    def port(port: Int): Builder = {
      this.port = CommonUtils.requirePositiveInt(port)
      this
    }

    def build(): JarStore = new JarStoreImpl(
      homeFolder = CommonUtils.requireNonEmpty(homeFolder),
      hostname = CommonUtils.requireNonEmpty(hostname),
      port = CommonUtils.requirePositiveInt(port)
    )
  }

  private[this] val LOG = Logger(classOf[JarStore])
  private[this] val ID_LENGTH = 10

  /**
    * This is a specific prefix which enables user to download binary of jar
    */
  private[this] val DOWNLOAD_JAR_PREFIX_PATH: String = "downloadJars"

  private[this] class JarStoreImpl(homeFolder: String, hostname: String, port: Int) extends JarStore {
    private[this] def toUrl(id: String): URL = new URL(
      s"http://$hostname:$port/${ConfiguratorApiInfo.V0}/$DOWNLOAD_JAR_PREFIX_PATH/$id.jar")

    override def route(implicit executionContext: ExecutionContext): Route =
      pathPrefix(DOWNLOAD_JAR_PREFIX_PATH / Segment) { idWithExtension =>
        // We force all url end with .jar
        if (!idWithExtension.endsWith(".jar")) complete(StatusCodes.NotFound -> s"$idWithExtension doesn't exist")
        else {
          val id = idWithExtension.substring(0, idWithExtension.indexOf(".jar"))
          // TODO: how to use future in getFromFile???
          getFromFile(Await.result(toFile(id), 30 seconds))
        }
      }

    private[this] val root: File = {
      val f = new File(homeFolder)
      if (!f.exists() && !f.mkdirs()) throw new IllegalStateException(s"failed to mkdir on $homeFolder")
      if (!f.isDirectory) throw new IllegalArgumentException(s"$homeFolder is a not a folder")
      f
    }

    /**
      * Create a folder base on the format : {root}/{group}/{id}
      *
      * @param group the group name
      * @param id the unique id
      * @return the folder file
      */
    private[this] def toFolder(group: String, id: String): File =
      new File(
        CommonUtils.path(root.getAbsolutePath, CommonUtils.requireNonEmpty(group), CommonUtils.requireNonEmpty(id)))

    override def toFile(id: String)(implicit executionContext: ExecutionContext): Future[File] =
      jarInfo(id).map(jarInfo =>
        CommonUtils.requireExist(new File(CommonUtils.requireExist(toFolder(jarInfo.group, id)), jarInfo.name)))

    override def add(file: File, newName: String, group: Option[String])(
      implicit executionContext: ExecutionContext): Future[JarInfo] =
      Future {
        val actualGroup = group.getOrElse(CommonUtils.randomString(ID_LENGTH))
        CommonUtils.requireNonEmpty(newName)
        CommonUtils.requireExist(file)
        def generateFolder(): File = {
          var rval: File = null
          while (rval == null) {
            val id = CommonUtils.randomString(ID_LENGTH)
            val f = toFolder(actualGroup, id)
            if (!f.exists()) {
              if (!f.mkdirs()) throw new IllegalArgumentException(s"fail to create folder on ${f.getAbsolutePath}")
              rval = f
            } else {
              rval = f
            }
          }
          rval
        }
        val folder = generateFolder()
        val id = folder.getName
        val newFile = new File(folder, newName)
        if (newFile.exists()) throw new IllegalArgumentException(s"${newFile.getAbsolutePath} already exists")
        CommonUtils.copyFile(file, newFile)
        LOG.debug(s"copy $file to $newFile")
        val plugin = JarInfo(
          id = id,
          name = newFile.getName,
          group = actualGroup,
          size = newFile.length(),
          url = toUrl(id),
          lastModified = newFile.lastModified()
        )
        LOG.info(s"add $plugin")
        plugin
      }

    override def jarInfos()(implicit executionContext: ExecutionContext): Future[Seq[JarInfo]] = Future.successful {
      // TODO: We should cache the plugins. because seeking to disk is a slow operation...
      root
        .listFiles()
        .filter(_.isDirectory)
        // we use the "group name" to group files
        .groupBy(_.getName)
        .flatMap {
          case (group, files) =>
            // for group folder, we list all actual jars
            files.map(file => file.listFiles().flatMap(_.listFiles())).flatMap { jars =>
              // for the same name of jar, get the latest modified jar
              jars
                .groupBy(_.getName)
                .map { case (_, sameJars) => sameJars.maxBy(_.lastModified()) }
                .map(
                  jar =>
                    JarInfo(
                      id = jar.getParentFile.getName,
                      name = jar.getName,
                      group = group,
                      size = jar.length(),
                      url = toUrl(jar.getParentFile.getName),
                      lastModified = jar.lastModified()
                  ))
            }
        }
        .toSeq
    }

    override def remove(id: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
      exist(id).flatMap {
        if (_) {
          CommonUtils.requireNonEmpty(id)
          jarInfo(id).map { info =>
            val file = toFolder(info.group, id)
            if (!file.exists()) throw new NoSuchElementException(s"$id doesn't exist")
            if (!file.isDirectory) throw new IllegalArgumentException(s"$id doesn't reference to a folder")
            CommonUtils.deleteFiles(file)
            true
          }
        } else Future.successful(false)
      }

    override def exist(id: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
      jarInfo(id).map(_ => true).recover {
        case _: Throwable =>
          false
      }

    override def update(id: String, file: File)(implicit executionContext: ExecutionContext): Future[JarInfo] =
      jarInfo(id).map { info =>
        CommonUtils.requireExist(file)
        val folder = toFolder(info.group, id)
        CommonUtils.deleteFiles(folder)
        if (!folder.mkdir()) throw new IllegalArgumentException(s"fail to create folder on $folder")
        val newFile = new File(folder, file.getName)
        if (newFile.exists()) throw new IllegalArgumentException(s"${newFile.getAbsolutePath} already exists")
        CommonUtils.copyFile(file, newFile)
        LOG.debug(s"copy $file to $newFile")
        val plugin = JarInfo(
          id = id,
          name = newFile.getName,
          group = info.group,
          size = newFile.length(),
          url = toUrl(id),
          lastModified = CommonUtils.current()
        )
        LOG.info(s"update $id by $plugin")
        plugin
      }

    override def rename(id: String, newName: String)(implicit executionContext: ExecutionContext): Future[JarInfo] =
      jarInfo(id).map { jarInfo =>
        CommonUtils.requireNonEmpty(newName)
        if (jarInfo.name == newName) jarInfo
        else {
          val folder = toFolder(jarInfo.group, id)
          val previousFIle = new File(folder, jarInfo.name)
          val newFile = new File(folder, newName)
          LOG.debug(s"copy file from $previousFIle to $newFile")
          CommonUtils.moveFile(previousFIle, newFile)
          JarInfo(
            id = id,
            name = newFile.getName,
            group = jarInfo.group,
            size = newFile.length(),
            url = toUrl(id),
            lastModified = CommonUtils.current()
          )
        }
      }

    override protected def close(): Unit = {
      // we clean nothing since the jars need to be available after configurator reopen
    }
  }
}
