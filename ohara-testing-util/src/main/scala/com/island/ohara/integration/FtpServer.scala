package com.island.ohara.integration
import java.util
import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.io.CloseOnce
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory
import org.apache.ftpserver.usermanager.impl.{BaseUser, WritePermission}

/**
  * a simple embedded ftp server providing 2 user (readonly and normal) in testing. There are some bound resources used
  * by this ftp server.
  * 1) home folder -> based on java.io.tmpdir with prefix - ftp
  * 2) port -> a random port
  * 3) host -> "localhost"
  * 4) writable user -> random user name and password
  * 5) readonly user -> random user name and password but the home folder is same as writable user
  *
  * all resources will be released by FtpServer#close(). For example, all data in home folder will be deleted
  */
trait FtpServer extends CloseOnce {
  def host: String
  def port: Int
  def user: String
  def password: String
}

object FtpServer {
  private[this] val COUNT = new AtomicInteger(0)
  def apply(): FtpServer = {
    val _port = availablePort
    val homeFolder = createTempDir("ftp")
    val userManagerFactory = new PropertiesUserManagerFactory()
    val userManager = userManagerFactory.createUserManager
    val _user = new BaseUser()
    _user.setName(s"user-${COUNT.getAndIncrement()}")
    _user.setAuthorities(util.Arrays.asList(new WritePermission()))
    _user.setEnabled(true)
    _user.setPassword(s"password-${COUNT.getAndIncrement()}")
    _user.setHomeDirectory(homeFolder.getAbsolutePath)
    userManager.save(_user)

    val listenerFactory = new ListenerFactory()
    listenerFactory.setPort(_port)

    val factory = new FtpServerFactory()
    factory.setUserManager(userManager)
    factory.addListener("default", listenerFactory.createListener)

    val server = factory.createServer
    server.start()

    new FtpServer {
      override protected def doClose(): Unit = {
        server.stop()
        deleteFile(homeFolder)
      }

      override def host: String = "localhost"

      override def port: Int = _port

      override def user: String = _user.getName

      override def password: String = _user.getPassword
    }
  }
}
