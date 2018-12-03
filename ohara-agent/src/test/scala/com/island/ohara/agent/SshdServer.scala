package com.island.ohara.agent

import java.io.{InputStream, OutputStream}
import java.util

import com.island.ohara.common.util.{CloseOnce, CommonUtil}
import org.apache.sshd.server.command.Command
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.shell.{ProcessShellCommandFactory, ProcessShellFactory}
import org.apache.sshd.server.{Environment, ExitCallback, SshServer}

/**
  * a embedded ssh server.
  */
trait SshdServer extends CloseOnce {

  /**
    * @return ssh server's hostname
    */
  def hostname: String

  /**
    * @return ssh server's port
    */
  def port: Int

  /**
    * @return ssh client's user
    */
  def user: String

  /**
    * @return ssh client's password
    */
  def password: String
}

object SshdServer {

  /**
    * used to handle the command manually. It is useful when the command you want to test doesn't work on local machine.
    */
  trait CommandHandler {

    /**
      * @param command will be executed on ssh server
      * @return true if this handler want to handle the command
      */
    def belong(command: String): Boolean

    /**
      * @param command will be executed on ssh server
      * @return response of the command
      */
    def execute(command: String): Seq[String]
  }

  private[agent] val SSHD_SERVER: String = "ohara.it.sshd"

  private[agent] def parseString(sshdString: String): (String, String, String, Int) = {
    // format => user:password@host:port
    try {
      val user = sshdString.split(":")(0)
      val password = sshdString.split(":")(1).split("@").head
      val host = sshdString.split(":")(1).split("@").last
      val port = sshdString.split(":").last.toInt
      (user, password, host, port)
    } catch {
      case e: Throwable => throw new IllegalArgumentException(s"invalid value from $SSHD_SERVER", e)
    }
  }

  def apply(): SshdServer = apply(sys.env.get(SSHD_SERVER))

  private[agent] def apply(sshdString: Option[String]): SshdServer = sshdString
    .map { s =>
      val (_user, _password, _hostname, _port) = parseString(s)
      new SshdServer {
        override def hostname: String = _hostname
        override def port: Int = _port
        override def user: String = _user
        override def password: String = _password
        override protected def doClose(): Unit = {}
      }
    }
    .getOrElse(local(0))

  /**
    * create a embedded sshd server bound on specified port
    * @param port listen port. If the port is smaller than zero, a random port is used in running sshd server
    * @param commandHandlers used in testing. Control the response for specified command. for example.
    *                       Map("hello" -> "world") means the command "hello" will get the response "world"
    * @return a embedded sshd server
    */
  def local(port: Int, commandHandlers: Seq[CommandHandler] = Seq.empty): SshdServer = {
    val _user = CommonUtil.uuid()
    val _password = CommonUtil.uuid()
    val sshd = SshServer.setUpDefaultServer()
    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider())
    sshd.setPasswordAuthenticator((username, password, _) => username == _user && password == _password)
    sshd.setShellFactory(new ProcessShellFactory(util.Arrays.asList("/bin/sh", "-i", "-l")))
    sshd.setCommandFactory(command => {
      commandHandlers
        .find(h => h.belong(command))
        .map {
          handler =>
            new Command {
              private[this] var out: OutputStream = _
              private[this] var callback: ExitCallback = _
              override def setInputStream(in: InputStream): Unit = {}

              override def setOutputStream(out: OutputStream): Unit = this.out = out

              override def setErrorStream(err: OutputStream): Unit = {}

              override def setExitCallback(callback: ExitCallback): Unit = this.callback = callback

              override def start(env: Environment): Unit = {
                handler.execute(command).foreach { s =>
                  out.write(s.getBytes)
                  // TODO: make it configurable...by chia
                  out.write("\n".getBytes)
                }
                callback.onExit(0)
              }
              override def destroy(): Unit = {}
            }

        }
        .getOrElse(ProcessShellCommandFactory.INSTANCE.createCommand(command))
    })
    sshd.setHost(CommonUtil.hostname())
    sshd.setPort(if (port <= 0) 0 else port)
    sshd.start()
    new SshdServer {
      override def hostname: String = sshd.getHost
      override def port: Int = sshd.getPort
      override def user: String = _user
      override def password: String = _password
      override protected def doClose(): Unit = sshd.close()
    }
  }

}
