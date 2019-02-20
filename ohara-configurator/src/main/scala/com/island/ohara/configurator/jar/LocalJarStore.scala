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
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce}
import com.typesafe.scalalogging.Logger
import org.apache.commons.io.FileUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import LocalJarStore._
private[configurator] abstract class LocalJarStore(val homeFolder: String) extends ReleaseOnce with JarStore {

  override def add(file: File, newName: String): Future[JarInfo] = Future {
    def generateFolder(): File = {
      var rval: File = null
      while (rval == null) {
        val id = CommonUtil.randomString(ID_LENGTH)
        val f = new File(homeFolder, id)
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
    FileUtils.copyFile(file, newFile)
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

  override def jarInfos(): Future[Seq[JarInfo]] = Future.successful {
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

  override def remove(id: String): Future[JarInfo] = jarInfo(id).map { jar =>
    val file = new File(homeFolder, id)
    if (!file.exists()) throw new NoSuchElementException(s"$id doesn't exist")
    if (!file.isDirectory) throw new IllegalArgumentException(s"$id doesn't reference to a folder")
    FileUtils.forceDelete(file)
    jar
  }

  override def url(id: String): Future[URL] = doUrls().map(_(id))

  override def urls(ids: Seq[String]): Future[Seq[URL]] = doUrls().map { idAndUrl =>
    ids.foreach(id => if (!idAndUrl.exists(_._1 == id)) throw new NoSuchElementException(s"$id doesn't exist"))
    idAndUrl.values.toSeq
  }

  override def urls(): Future[Seq[URL]] = doUrls().map(_.values.toSeq)

  protected def doUrls(): Future[Map[String, URL]]

  override def exist(id: String): Future[Boolean] = if (CommonUtil.isEmpty(id))
    Future.failed(new IllegalArgumentException("id can't by empty"))
  else Future.successful(new File(homeFolder, id).exists())

  override def update(id: String, file: File): Future[JarInfo] = if (file == null)
    Future.failed(new IllegalArgumentException(s"file can't be null"))
  else
    exist(id).flatMap(
      if (_) try {
        CommonUtil.deleteFiles(new File(homeFolder, id))
        val folder = new File(homeFolder, id)
        if (!folder.mkdir()) throw new IllegalArgumentException(s"fail to create folder on $folder")
        val newFile = new File(folder, file.getName)
        if (newFile.exists()) throw new IllegalArgumentException(s"${newFile.getAbsolutePath} already exists")
        FileUtils.copyFile(file, newFile)
        LOG.debug(s"copy $file to $newFile")
        val plugin = JarInfo(
          id = id,
          name = newFile.getName,
          size = newFile.length(),
          lastModified = CommonUtil.current()
        )
        LOG.info(s"update $id by $plugin")
        Future.successful(plugin)
      } catch {
        case e: Throwable => Future.failed(e)
      } else Future.failed(new IllegalArgumentException(s"$id doesn't exist"))
    )
}

object LocalJarStore {
  private val LOG = Logger(LocalJarStore.getClass)
  private val ID_LENGTH = 10
}
