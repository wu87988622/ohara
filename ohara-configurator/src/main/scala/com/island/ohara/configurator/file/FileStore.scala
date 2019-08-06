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

package com.island.ohara.configurator.file

import java.io.File
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.Objects

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.island.ohara.client.configurator.ConfiguratorApiInfo
import com.island.ohara.client.configurator.v0.FileInfoApi
import com.island.ohara.client.configurator.v0.FileInfoApi.FileInfo
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.file.FileStore.FileInfoCreator
import com.island.ohara.kafka.connector.json.ObjectKey
import com.typesafe.scalalogging.Logger
import spray.json.JsValue

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Plugin store used to keep the custom plugin (connector or streamApp) and provide a way to remote node to get the plugin.
  * In order to simplify the plugin store, the "way" used to get the plugin are format to URL. Also, the default implementation is based
  * on ftp, which can be load by java dynamical call.
  *
  * The hierarchy of file system is shown below.
  * root folder
  *   |
  *   | --- group folder (group0)
  *   |    |
  *   |    | --- file (a.jar)
  *   |    | --- tags file (a.jar.tags)
  *   |    | --- file (b.jar)
  *   |    | --- tags file (b.jar.tags)
  *   |
  *   | --- group folder (group1)
  *   |    |
  *   |    | --- file (a.jar)
  *   |    | --- tags file (a.jar.tags)
  *
  *   The above files generates following jar infos
  *   1) group = group0, name = a.jar
  *   2) group = group0, name = b.jar
  *   3) group = group1, name = a.jar
  */
trait FileStore extends Releasable {

  /**
    * start a progress to add/update file to this store
    * @return FileInfoCreator
    */
  def fileInfoCreator: FileInfoCreator

  /**
    * remove a existed file from file store
    * @param key object key
    */
  def remove(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Boolean]

  /**
    * retrieve the information of file
    * @param key object key
    * @return file description
    */
  def fileInfo(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[FileInfo] =
    fileInfos().map(infos => {
      infos.find(info => info.key == key).getOrElse(throw new NoSuchElementException(s"$key does not exist"))
    })

  /**
    * retrieve all files from file store
    * @param executionContext execution context
    * @return files description
    */
  def fileInfos()(implicit executionContext: ExecutionContext): Future[Seq[FileInfo]]

  /**
    * retrieve all files with filter group from file store
    * @param group the filter group name
    * @param executionContext execution context
    * @return files description
    */
  def fileInfos(group: String)(implicit executionContext: ExecutionContext): Future[Seq[FileInfo]] =
    fileInfos().map(infos => infos.filter(_.group == group))

  /**
    * check the existence of a file
    * @param key object key
    * @param executionContext thread pool
    * @return true if file exists. otherwise, false
    */
  def exist(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Boolean]

  /**
    * Create a local file storing the file.
    * NOTED: you should NOT modify the file since the returned file may be referenced to true data.
    * @param key object key
    * @param executionContext thread pool
    * @return local file
    */
  def toFile(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[File]

  /**
    * update the tags of an existent file
    * @param key object key
    * @param tags new tags
    * @param executionContext thread pool
    * @return updated file
    */
  def updateTags(key: ObjectKey, tags: Map[String, JsValue])(
    implicit executionContext: ExecutionContext): Future[FileInfo]

  /**
    * generate the route offering the file download of this file store
    * @param executionContext thread pool
    * @return server route
    */
  def route(implicit executionContext: ExecutionContext): server.Route
}

object FileStore {
  def builder: Builder = new Builder

  private[this] val GROUP_KEY = com.island.ohara.client.configurator.v0.GROUP_KEY
  class Builder private[FileStore] extends com.island.ohara.common.pattern.Builder[FileStore] {
    private[this] var homeFolder: String = _
    private[this] var hostname: String = _
    private[this] var port: Option[Int] = None
    private[this] var acceptedExtensions: Set[String] = Set.empty

    def homeFolder(homeFolder: String): Builder = {
      this.homeFolder = CommonUtils.requireNonEmpty(homeFolder)
      this
    }

    /**
      * set the hostname added to the URL. Noted that file store won't instantiate a server internally
      * @param hostname hostname of url
      * @return this builder
      */
    def hostname(hostname: String): Builder = {
      this.hostname = CommonUtils.requireNonEmpty(hostname)
      this
    }

    /**
      * set the port added to the URL. Noted that file store won't instantiate a server internally
      * @param port port of url
      * @return this builder
      */
    def port(port: Int): Builder = {
      this.port = Some(CommonUtils.requireConnectionPort(port))
      this
    }

    import scala.collection.JavaConverters._

    /**
      * set the legal extension to this file store. The files having illegal extension will be excluded when file store is loading file from filesystem, and
      * you can't add any illegal file to this file store. However, the previous illegal files are not deleted.
      * @param acceptedExtensions accepted extension to file
      * @return this builder
      */
    @Optional("by default all extension are acceptable. (excluding \"tags\" as it is a keyword in file store)")
    def acceptedExtensions(acceptedExtensions: Set[String]): Builder = {
      this.acceptedExtensions = CommonUtils.requireNonEmpty(acceptedExtensions.asJava).asScala.toSet[String].map {
        extension =>
          if (extension.contains("."))
            throw new IllegalArgumentException(
              "the input extension does not include dot. " +
                "For example, accept extension:\"jar\" makes file store to accept \"aaa.jar\" file. " +
                "By contrast, \".jar\" causes this exception :(")
          extension
      }
      this
    }

    override def build: FileStore = new FileStoreImpl(
      homeFolder = CommonUtils.requireNonEmpty(homeFolder),
      hostname = CommonUtils.requireNonEmpty(hostname),
      port =
        port.map(CommonUtils.requireConnectionPort).getOrElse(throw new NullPointerException("where is your port???")),
      acceptedExtensions = Objects.requireNonNull(acceptedExtensions)
    )
  }

  trait FileInfoCreator extends com.island.ohara.common.pattern.Creator[Future[FileInfo]] {
    private[this] var file: File = _
    private[this] var group: String = FileInfoApi.GROUP_DEFAULT
    private[this] var name: String = _
    private[this] var tags: Map[String, JsValue] = Map.empty
    private[this] var threadPool: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    def file(file: File): FileInfoCreator = {
      this.file = CommonUtils.requireFile(file)
      if (name == null) name(file.getName)
      this
    }

    @Optional("Default group is Data.GROUP_DEFAULT")
    def group(group: String): FileInfoCreator = {
      this.group = CommonUtils.requireNonEmpty(group)
      this
    }

    @Optional("Default name is the name of input file")
    def name(name: String): FileInfoCreator = {
      this.name = CommonUtils.requireNonEmpty(name)
      this
    }

    @Optional("Default value is empty")
    def tags(tags: Map[String, JsValue]): FileInfoCreator = {
      this.tags = Objects.requireNonNull(tags)
      this
    }

    @Optional("Default thread pool is scala's global pool")
    def threadPool(threadPool: ExecutionContext): FileInfoCreator = {
      this.threadPool = Objects.requireNonNull(threadPool)
      this
    }

    override def create(): Future[FileInfo] = doCreate(
      file = CommonUtils.requireFile(file),
      key = ObjectKey.of(group, name),
      tags = Objects.requireNonNull(tags),
      threadPool = Objects.requireNonNull(threadPool)
    )

    protected def doCreate(file: File,
                           key: ObjectKey,
                           tags: Map[String, JsValue],
                           threadPool: ExecutionContext): Future[FileInfo]
  }

  private[this] val LOG = Logger(classOf[FileStore])

  /**
    * This is a specific prefix which enables user to download binary of file
    */
  private[this] val DOWNLOAD_FILE_PREFIX_PATH: String = "downloadFiles"

  private[this] class FileStoreImpl(homeFolder: String, hostname: String, port: Int, acceptedExtensions: Set[String])
      extends FileStore {
    private[this] def toUrl(key: ObjectKey): URL = new URL(
      s"http://$hostname:$port/${ConfiguratorApiInfo.V0}/$DOWNLOAD_FILE_PREFIX_PATH/${key.name()}?$GROUP_KEY=${key.group()}")

    // ----------------------[helper methods for tags]----------------------//
    private[this] val charset: Charset = Charset.forName("UTF-8")
    private[this] def toTagsFileName(file: File): String = toTagsFileName(file.getName)
    private[this] def toTagsFileName(fileName: String): String = if (fileName.endsWith(".tag"))
      throw new IllegalArgumentException(s"the input file name can't end with .tags")
    else s"$fileName.tags"
    private[this] def isAcceptedFile(file: File): Boolean = isAcceptedFilename(file.getName)

    /**
      *
      * @param filename file name
      * @return true if 1) input name has extension 2) the extension is not equal to "tags" 3) the extension is matched by input accepted extensions
      */
    private[this] def isAcceptedFilename(filename: String): Boolean =
      CommonUtils.hasExtension(filename) &&
        CommonUtils.extension(filename) != "tags" &&
        (acceptedExtensions.isEmpty || acceptedExtensions.contains(CommonUtils.extension(filename)))

    override def updateTags(key: ObjectKey, tags: Map[String, JsValue])(
      implicit executionContext: ExecutionContext): Future[FileInfo] = fileInfo(key).flatMap { fileInfo =>
      Future {
        // ok, the file info exists!!! we are goint to overwrite the tags file now.
        val tagsFile = new File(toFolder(key.group()), toTagsFileName(key.name))
        Files.write(tagsFile.toPath, FileInfoApi.toString(Objects.requireNonNull(tags)).getBytes(charset))
        fileInfo.copy(tags = tags)
      }
    }

    override def fileInfoCreator: FileInfoCreator =
      (file: File, key: ObjectKey, tags: Map[String, JsValue], threadPool: ExecutionContext) =>
        Future {
          val folder = toFolder(key.group())
          if (folder.isFile) throw new IllegalArgumentException(s"group:${key.group()} is associated to a file")
          if (!folder.exists() && !folder.mkdir())
            throw new IllegalArgumentException(s"failed to make folder on ${folder.getAbsolutePath}")
          val newFile = new File(folder, key.name())
          if (!isAcceptedFile(newFile))
            throw new IllegalArgumentException(
              s"$newFile has illegal extension. the accepted extension are ${acceptedExtensions.mkString(",")}")
          if (newFile.exists()) {
            // delete the existed old file
            CommonUtils.deleteFiles(newFile)
          }
          CommonUtils.copyFile(file, newFile)
          LOG.debug(s"copy $file to $newFile")
          Files.write(new File(folder, toTagsFileName(newFile)).toPath, FileInfoApi.toString(tags).getBytes(charset))
          val plugin = FileInfo(
            name = key.name(),
            group = key.group(),
            size = newFile.length(),
            url = toUrl(key),
            lastModified = newFile.lastModified(),
            tags = tags
          )
          LOG.info(s"add $plugin")
          plugin
        }(threadPool)

    override def route(implicit executionContext: ExecutionContext): Route =
      pathPrefix(DOWNLOAD_FILE_PREFIX_PATH / Segment) { name =>
        parameter(GROUP_KEY ?) { groupOption =>
          val key = ObjectKey.of(groupOption.getOrElse(FileInfoApi.GROUP_DEFAULT), name)
          if (!isAcceptedFilename(name))
            complete(
              StatusCodes.NotFound -> s"$name is illegal. Accepted extensions are ${acceptedExtensions.mkString(",")}")
          // TODO: how to use future in getFromFile???
          else getFromFile(Await.result(toFile(key), 30 seconds))
        }

      }

    private[this] val root: File = {
      val f = new File(homeFolder)
      if (!f.exists() && !f.mkdirs()) throw new IllegalStateException(s"failed to mkdir on $homeFolder")
      if (!f.isDirectory) throw new IllegalArgumentException(s"$homeFolder is a not a folder")
      f
    }

    /**
      * Create a folder base on the format : {root}/{group}/
      *
      * @param group the group name
      * @return the folder file
      */
    private[this] def toFolder(group: String): File =
      new File(CommonUtils.path(root.getAbsolutePath, CommonUtils.requireNonEmpty(group)))

    override def toFile(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[File] =
      fileInfo(key).map(jarInfo =>
        CommonUtils.requireExist(new File(CommonUtils.requireExist(toFolder(jarInfo.group)), key.name())))

    override def fileInfos()(implicit executionContext: ExecutionContext): Future[Seq[FileInfo]] =
      Future.successful {
        root
          .listFiles()
          .filter(_.isDirectory)
          .flatMap { folder =>
            val fs = folder.listFiles()
            if (fs == null) Seq.empty
            else {
              fs.filter(isAcceptedFile)
                .flatMap { file =>
                  val tagsFile = new File(folder, toTagsFileName(file))
                  if (tagsFile.isFile) {
                    val tags = FileInfoApi.toTags(new String(Files.readAllBytes(tagsFile.toPath)))
                    Some((folder.getName, file, tags))
                  } else None
                }
                .toSeq
            }
          }
          .map {
            case (group, file, tags) =>
              FileInfo(
                name = file.getName,
                group = group,
                size = file.length(),
                url = toUrl(ObjectKey.of(group, file.getName)),
                lastModified = file.lastModified(),
                tags = tags
              )
          }
          .toSeq
      }

    override def remove(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Boolean] =
      fileInfo(key)
        .map { info =>
          val groupFolder = toFolder(info.group)
          if (groupFolder.exists()) {
            if (!groupFolder.isDirectory) throw new IllegalArgumentException(s"$key doesn't reference to a folder")
            val f = new File(groupFolder, key.name())
            val tagsFile = new File(groupFolder, toTagsFileName(key.name()))
            if (f.exists()) CommonUtils.deleteFiles(f)
            if (tagsFile.exists()) CommonUtils.deleteFiles(tagsFile)
          }
          true
        }
        .recover {
          // the file does not exist so we do nothing for it.
          case _: NoSuchElementException => false
        }

    override def exist(key: ObjectKey)(implicit executionContext: ExecutionContext): Future[Boolean] =
      fileInfo(key).map(_ => true).recover {
        case _: Throwable =>
          false
      }

    override protected def close(): Unit = {
      // we clean nothing since the jars need to be available after configurator reopen
    }
  }
}
