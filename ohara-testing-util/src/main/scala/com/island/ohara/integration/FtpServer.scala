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
  def writableUser: User
  def readonlyUser: User
  def host: String
  def port: Int
}

object FtpServer {
  private[this] val COUNT = new AtomicInteger(0)
  def apply(): FtpServer = {
    val _port = availablePort
    val homeFolder = createTempDir("ftp")
    val userManagerFactory = new PropertiesUserManagerFactory()
    val userManager = userManagerFactory.createUserManager
    val user = new BaseUser()
    user.setName(s"user-${COUNT.getAndIncrement()}")
    user.setAuthorities(util.Arrays.asList(new WritePermission()))
    user.setEnabled(true)
    user.setPassword(s"password-${COUNT.getAndIncrement()}")
    user.setHomeDirectory(homeFolder.getAbsolutePath)
    userManager.save(user)

    val _readonlyUser = new BaseUser()
    _readonlyUser.setName(s"user-${COUNT.getAndIncrement()}")
    _readonlyUser.setEnabled(true)
    _readonlyUser.setPassword(s"password-${COUNT.getAndIncrement()}")
    _readonlyUser.setHomeDirectory(homeFolder.getAbsolutePath)
    userManager.save(_readonlyUser)

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

      override def writableUser: User = User(user.getName, user.getPassword)

      override def readonlyUser: User = User(_readonlyUser.getName, _readonlyUser.getPassword)
    }
  }
}
