package com.island.ohara.agent.plugin
import java.io.File
import java.net.URL

import com.island.ohara.client.ConfiguratorJson.PluginDescription
import com.island.ohara.common.util.{CommonUtil, Releasable}

import scala.concurrent.Future

/**
  * Plugin store used to keep the custom plugin (connector or streamapp) and provide a way to remote node to get the plugin.
  * In order to simplify the plugin store, the "way" used to get the plugin are format to URL. Also, the default implementation is based
  * on ftp, which can be load by java dynamical call.
  */
trait PluginStore extends Releasable with Iterable[PluginDescription] {

  /**
    * add a jar into store. This is a async method so you need to check the result of future.
    * @param file jar file
    * @return a async thread which is doing the upload
    */
  def add(file: File): Future[PluginDescription]

  /**
    * remove a existed jar file from plugin store
    * @param id jar file's id
    */
  def remove(id: String): Unit

  /**
    * update the plugin
    * @param id plugin's id
    * @param file new plugin
    * @return a async thread which is doing the upload
    */
  def update(id: String, file: File): Future[PluginDescription]

  /**
    * retrieve the information of plugin
    * @param id plugin's id
    * @return plugin description
    */
  def pluginDescription(id: String): PluginDescription = if (CommonUtil.isEmpty(id))
    throw new IllegalArgumentException(s"$id can't be empty")
  else find(_.id == id).get

  /**
    * generate a downloadable remote resource.
    * @param id plugin's id
    * @return url connection
    */
  def url(id: String): URL
}

object PluginStore {
  def ftp(homeFolder: String, commandPort: Int, dataPorts: Array[Int]): PluginStore =
    new FtpPluginStore(homeFolder, commandPort, dataPorts)

}
