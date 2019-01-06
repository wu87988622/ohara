package com.island.ohara.agent.plugin
import java.io.File
import java.net.URL
import java.nio.file.Paths

import com.island.ohara.agent.plugin.FtpPluginStore._
import com.island.ohara.client.configurator.v0.ConnectorApi.PluginDescription
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce}
import com.typesafe.scalalogging.Logger
import org.apache.commons.io.FileUtils
import org.apache.ftpserver.ftplet.UserManager
import org.apache.ftpserver.listener.{Listener, ListenerFactory}
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.{ConnectionConfigFactory, DataConnectionConfigurationFactory, FtpServer, FtpServerFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * a plugin store based on ftp server. All plugins (jar files) are stored locally, and the URL are on ftp protocol.
  * The embedded ftp server will generated a read-only username/password, and they are attached to the URL so people can read the jar
  * through url (ftp protocol).
  * NOTED: this plugin store implementation doesn't guarantee the durability of plugins so user has got to keep the plugins manually.
  * @param homeFolder the root folder of this store.
  * @param commandPort the public ftp control port
  * @param dataPorts the public ftp data port
  */
private[plugin] class FtpPluginStore(homeFolder: String, commandPort: Int, dataPorts: Array[Int])
    extends ReleaseOnce
    with PluginStore {
  private[this] def assertNotEmpty(s: String, msg: => String): Unit =
    if (CommonUtil.isEmpty(s)) throw new IllegalArgumentException(msg)
  private[this] val (ftpServer: FtpServer, userName: String, password: String, port: Int) = {
    assertNotEmpty(homeFolder, "home folder can't be empty")
    if (homeFolder.length != 1 && homeFolder.endsWith(File.separator))
      throw new IllegalArgumentException(s"$homeFolder can end with ${File.separator}")
    if (!Paths.get(homeFolder).isAbsolute) throw new IllegalArgumentException(s"$homeFolder should be an absolute path")
    if (dataPorts == null || dataPorts.isEmpty) throw new IllegalArgumentException(s"data ports can't be empty")
    val userManagerFactory: PropertiesUserManagerFactory = new PropertiesUserManagerFactory
    val userManager: UserManager = userManagerFactory.createUserManager
    val userName = CommonUtil.randomString(USER_NAME_LENGTH)
    val password = CommonUtil.randomString(PASSWORD_LENGTH)
    val user: BaseUser = new BaseUser
    user.setName(userName)
    user.setPassword(password)
    // NOTED: DON'T grant the write permission to the user!
    // user.setAuthorities(java.util.Collections.singletonList(new WritePermission))
    user.setEnabled(true)
    user.setHomeDirectory(homeFolder)
    userManager.save(user)
    val listenerFactory: ListenerFactory = new ListenerFactory
    listenerFactory.setPort(commandPort)
    LOG.info(s"command port:$commandPort")
    val dataConnectionConfig: DataConnectionConfigurationFactory = new DataConnectionConfigurationFactory

    dataConnectionConfig.setActiveEnabled(false)
    val passivePorts = dataPorts.map(CommonUtil.resolvePort).mkString(",")
    LOG.info(s"passive ports:$passivePorts")
    dataConnectionConfig.setPassivePorts(passivePorts)
    listenerFactory.setDataConnectionConfiguration(dataConnectionConfig.createDataConnectionConfiguration)

    val connectionConfig = new ConnectionConfigFactory
    // the number of threads should be same to number of data ports. Otherwise, connection may fail to get free data port
    // and then cause the failed connection.
    connectionConfig.setMaxThreads(dataPorts.length)

    val listener: Listener = listenerFactory.createListener
    val factory: FtpServerFactory = new FtpServerFactory
    factory.setUserManager(userManager)
    factory.addListener("default", listener)
    factory.setConnectionConfig(connectionConfig.createConnectionConfig())
    val server: FtpServer = factory.createServer
    server.start()
    (server, userName, password, listener.getPort)
  }
  override protected def doClose(): Unit = if (ftpServer != null) ftpServer.stop()
  override def add(file: File): Future[PluginDescription] = Future {
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
    val newFile = new File(folder, file.getName)
    if (newFile.exists()) throw new IllegalArgumentException(s"${newFile.getAbsolutePath} already exists")
    FileUtils.copyFile(file, newFile)
    LOG.debug(s"copy $file to $newFile")
    val plugin = PluginDescription(
      id = id,
      name = newFile.getName,
      size = newFile.length(),
      lastModified = CommonUtil.current()
    )
    LOG.info(s"add $plugin")
    plugin
  }

  override def remove(id: String): Unit = {
    assertNotEmpty(id, "id can't be empty")
    val file = new File(homeFolder, id)
    if (!file.exists()) throw new IllegalArgumentException(s"$id doesn't exist")
    if (!file.isDirectory) throw new IllegalArgumentException(s"$id doesn't reference to a folder")
    FileUtils.forceDelete(file)
  }

  override def iterator: Iterator[PluginDescription] = {
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
              PluginDescription(
                id = folder.getName,
                name = jar.getName,
                size = jar.length(),
                lastModified = jar.lastModified()
              ))
          }
        }
        .toSeq
        .iterator
    else Iterator.empty
  }
  override def url(id: String): URL = {
    val plugin = pluginDescription(id)
    // NOTED: we replace hostname by actual ip address so we don't need to add route to worker containers.
    val hostname = CommonUtil.address(CommonUtil.hostname())
    // NOTED: DON'T append the homeFolder into the path since homeFolder is "root" of ftp server.
    val path = s"ftp://$userName:$password@$hostname:$port/$id/${plugin.name}"
    new URL(path)
  }

  override def update(id: String, file: File): Future[PluginDescription] = if (exists(_.id == id)) {
    if (file == null) throw new IllegalArgumentException(s"file can't be null")
    CommonUtil.deleteFiles(new File(homeFolder, id))
    Future {
      val folder = new File(homeFolder, id)
      if (!folder.mkdir()) throw new IllegalArgumentException(s"fail to create folder on $folder")
      val newFile = new File(folder, file.getName)
      if (newFile.exists()) throw new IllegalArgumentException(s"${newFile.getAbsolutePath} already exists")
      FileUtils.copyFile(file, newFile)
      LOG.debug(s"copy $file to $newFile")
      val plugin = PluginDescription(
        id = id,
        name = newFile.getName,
        size = newFile.length(),
        lastModified = CommonUtil.current()
      )
      LOG.info(s"update $id by $plugin")
      plugin
    }
  } else throw new IllegalArgumentException(s"$id doesn't exist")
}

object FtpPluginStore {
  private val LOG = Logger(FtpPluginStore.getClass)
  private val ID_LENGTH = 10
  private val USER_NAME_LENGTH = 10
  private val PASSWORD_LENGTH = 10
}
