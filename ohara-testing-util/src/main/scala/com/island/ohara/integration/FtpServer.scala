package com.island.ohara.integration
import java.util
import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.io.{CloseOnce, IoUtil}
import org.apache.ftpserver.{DataConnectionConfigurationFactory, FtpServerFactory}
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory
import org.apache.ftpserver.usermanager.impl.{BaseUser, WritePermission}

/**
  * a simple embedded ftp server providing 1 writable user. The home folder is based on java.io.tmpdir with prefix - ftp
  * 1) port -> a random port
  * 2) host -> "localhost"
  * 3) user -> a writable account
  * 4) password -> a writable account
  *
  * all resources will be released by FtpServer#close(). For example, all data in home folder will be deleted
  *
  * If ohara.it.ftp exists in env variables, local ftp server is not created.
  */
trait FtpServer extends CloseOnce {
  def host: String
  def port: Int
  def user: String
  def password: String

  /**
    * If the ftp server is in passive mode, the port is used to transfer data
    * @return data port
    */
  def dataPort: Int

  /**
    * @return true if this ftp server is generated locally.
    */
  def isLocal: Boolean
}

object FtpServer {
  private[integration] val FTP_SERVER: String = "ohara.it.ftp"

  private[this] val COUNT = new AtomicInteger(0)

  private[integration] def parseString(ftpString: String): (String, String, String, Int) = {
    // format => user:password@host:port
    try {
      val user = ftpString.split(":").head
      val password = ftpString.split("@").head.split(":").last
      val host = ftpString.split("@").last.split(":").head
      val port = ftpString.split("@").last.split(":").last.toInt
      (user, password, host, port)
    } catch {
      case e: Throwable => throw new IllegalArgumentException(s"invalid value of $FTP_SERVER", e)
    }
  }

  /**
    * create an embedded ftp server with specific port
    * @param commandPort bound port used to control
    * @param dataPort bound port used to transfer data
    * @return an embedded ftp server
    */
  def local(commandPort: Int, dataPort: Int): FtpServer = {
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
    listenerFactory.setPort(commandPort)
    val connectionConfig = new DataConnectionConfigurationFactory()
    connectionConfig.setActiveEnabled(false)
    connectionConfig.setPassivePorts(if (dataPort <= 0) availablePort().toString else dataPort.toString)
    listenerFactory.setDataConnectionConfiguration(connectionConfig.createDataConnectionConfiguration())

    val listener = listenerFactory.createListener
    val factory = new FtpServerFactory()
    factory.setUserManager(userManager)
    factory.addListener("default", listener)
    val server = factory.createServer
    server.start()
    new FtpServer {
      override protected def doClose(): Unit = {
        server.stop()
        deleteFile(homeFolder)
      }

      override def host: String = IoUtil.hostname

      override def port: Int = listener.getPort

      override def user: String = _user.getName

      override def password: String = _user.getPassword
      override def isLocal: Boolean = true
      override def dataPort: Int = connectionConfig.getPassivePorts.toInt
    }
  }

  def apply(): FtpServer = apply(sys.env.get(FTP_SERVER))

  private[integration] def apply(ftpServer: Option[String]): FtpServer =
    ftpServer
      .map { s =>
        val (_user, _password, _host, _port) = parseString(s)
        new FtpServer {
          override def host: String = _host
          override def port: Int = _port
          override def user: String = _user
          override def password: String = _password
          override protected def doClose(): Unit = {}
          override def isLocal: Boolean = false
          override def dataPort: Int =
            throw new UnsupportedOperationException("TODO: can't get data port from actual ftp server")
        }
      }
      .getOrElse(local(0, 0))
}
