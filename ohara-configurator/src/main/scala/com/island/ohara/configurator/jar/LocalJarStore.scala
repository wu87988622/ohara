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
import com.island.ohara.common.util.{CommonUtils, ReleaseOnce}
import com.island.ohara.configurator.jar.LocalJarStore._
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}
private[configurator] class LocalJarStore(val homeFolder: String,
                                          urlPrefix: String,
                                          advertisedHostname: String,
                                          advertisedPort: Int)
    extends ReleaseOnce
    with JarStore {

  private[this] def toFolder(id: String): File = new File(homeFolder, CommonUtils.requireNonEmpty(id))

  override def urls(implicit executionContext: ExecutionContext): Future[Map[String, URL]] = jarInfos.map(_.map {
    plugin =>
      plugin.id -> new URL(s"http://$advertisedHostname:$advertisedPort/$urlPrefix/${plugin.id}.jar")
  }.toMap)

  override def toFile(id: String)(implicit executionContext: ExecutionContext): Future[File] =
    jarInfo(id).map(jarInfo => CommonUtils.requireExist(new File(CommonUtils.requireExist(toFolder(id)), jarInfo.name)))

  override def add(file: File, newName: String)(implicit executionContext: ExecutionContext): Future[JarInfo] =
    Future {
      CommonUtils.requireNonEmpty(newName)
      CommonUtils.requireExist(file)
      def generateFolder(): File = {
        var rval: File = null
        while (rval == null) {
          val id = CommonUtils.randomString(ID_LENGTH)
          val f = toFolder(id)
          if (!f.exists()) {
            if (!f.mkdir()) throw new IllegalArgumentException(s"fail to create folder on ${f.getAbsolutePath}")
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
        size = newFile.length(),
        lastModified = newFile.lastModified()
      )
      LOG.info(s"add $plugin")
      plugin
    }

  override def jarInfos(implicit executionContext: ExecutionContext): Future[Seq[JarInfo]] = Future.successful {
    // TODO: We should cache the plugins. because seeking to disk is a slow operation...
    val root = new File(homeFolder)
    val files = root.listFiles()
    if (files != null)
      files
        .filter(_.isDirectory)
        .flatMap { folder =>
          val jars = folder.listFiles()
          if (jars == null || jars.isEmpty) None
          else {
            val jar = jars.maxBy(_.lastModified())
            Some(
              JarInfo(
                id = folder.getName,
                name = jar.getName,
                size = jar.length(),
                lastModified = jar.lastModified()
              ))
          }
        }
        .toSeq
    else Seq.empty
  }

  override def remove(id: String)(implicit executionContext: ExecutionContext): Future[JarInfo] =
    jarInfo(id).map { jar =>
      CommonUtils.requireNonEmpty(id)
      val file = toFolder(id)
      if (!file.exists()) throw new NoSuchElementException(s"$id doesn't exist")
      if (!file.isDirectory) throw new IllegalArgumentException(s"$id doesn't reference to a folder")
      CommonUtils.deleteFiles(file)
      jar
    }

  override def exist(id: String)(implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.successful(toFolder(CommonUtils.requireNonEmpty(id)).exists())

  override def update(id: String, file: File)(implicit executionContext: ExecutionContext): Future[JarInfo] =
    jarInfo(id).map { _ =>
      CommonUtils.requireExist(file)
      val folder = toFolder(id)
      CommonUtils.deleteFiles(folder)
      if (!folder.mkdir()) throw new IllegalArgumentException(s"fail to create folder on $folder")
      val newFile = new File(folder, file.getName)
      if (newFile.exists()) throw new IllegalArgumentException(s"${newFile.getAbsolutePath} already exists")
      CommonUtils.copyFile(file, newFile)
      LOG.debug(s"copy $file to $newFile")
      val plugin = JarInfo(
        id = id,
        name = newFile.getName,
        size = newFile.length(),
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
        val folder = toFolder(id)
        val previousFIle = new File(folder, jarInfo.name)
        val newFile = new File(folder, newName)
        LOG.debug(s"copy file from $previousFIle to $newFile")
        CommonUtils.moveFile(previousFIle, newFile)
        JarInfo(
          id = id,
          name = newFile.getName,
          size = newFile.length(),
          lastModified = CommonUtils.current()
        )
      }
    }

  override protected def doClose(): Unit = {
    // do nothing
  }
}

object LocalJarStore {
  private val LOG = Logger(LocalJarStore.getClass)
  private val ID_LENGTH = 10
}
