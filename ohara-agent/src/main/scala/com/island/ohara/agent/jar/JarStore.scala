package com.island.ohara.agent.jar
import java.io.File
import java.net.URL

import com.island.ohara.client.configurator.v0.JarApi.JarInfo
import com.island.ohara.common.util.{CommonUtil, Releasable}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

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
  def add(file: File): Future[JarInfo] = add(file, file.getName)

  /**
    * add a jar into store. This is a async method so you need to check the result of future.
    * @param file jar file
    * @param newName new name of jar file
    * @return a async thread which is doing the upload
    */
  def add(file: File, newName: String): Future[JarInfo]

  /**
    * remove a existed jar file from jar store
    * @param id jar file's id
    */
  def remove(id: String): Future[JarInfo]

  /**
    * update the jar
    * @param id jar's id
    * @param file new jar
    * @return a async thread which is doing the upload
    */
  def update(id: String, file: File): Future[JarInfo]

  /**
    * retrieve the information of jar
    * @param id jar's id
    * @return jar description
    */
  def jarInfo(id: String): Future[JarInfo] = if (CommonUtil.isEmpty(id))
    Future.failed(new IllegalArgumentException(s"$id can't be empty"))
  else jarInfos().map(_.find(_.id == id).head)

  def jarInfos(ids: String): Future[Seq[JarInfo]] = jarInfos().map(_.filter(p => ids.contains(p.id)))

  def jarInfos(): Future[Seq[JarInfo]]

  /**
    * generate a downloadable remote resource.
    * @param id jar's id
    * @return url connection
    */
  def url(id: String): Future[URL]

  def urls(ids: Seq[String]): Future[Seq[URL]]

  def urls(): Future[Seq[URL]]

  def exist(id: String): Future[Boolean]

  def nonExist(id: String): Future[Boolean] = exist(id).map(!_)
}

object JarStore {

  /**
    * create a store based on ftp interface. All jars are stored at input - home folder, and all read to jar are formatted
    * to ftp url.
    * @param homeFolder location to store jars
    * @param numberOfThreads max connection to transfer data
    * @return jar store
    */
  def ftp(homeFolder: String, numberOfThreads: Int): JarStore =
    new FtpJarStore(homeFolder, 0, Seq.fill(numberOfThreads)(0))
}
