package com.island.ohara.agent

import java.io.ByteArrayOutputStream
import java.nio.charset.{Charset, StandardCharsets}
import java.util.Objects

import com.island.ohara.common.util.ReleaseOnce
import com.typesafe.scalalogging.Logger
import org.apache.sshd.client.SshClient

/**
  * represent a remote node. Default implementation is based on ssh.
  */
trait Agent extends ReleaseOnce {

  /**
    * execute the command by ssh
    * @param command command
    * @return response from remote node
    */
  def execute(command: String): Option[String]
}

/**
  * used to execute command on remote node.
  * the default implementation is based on ssh
  */
object Agent {

  def builder(): Builder = new Builder()

  class Builder private[agent] {
    private[this] var hostname: String = _
    private[this] var port: Int = 22
    private[this] var user: String = _
    private[this] var password: String = _
    private[this] var charset = StandardCharsets.US_ASCII

    /**
      * set remote hostname
      * @param hostname hostname
      * @return this builder
      */
    def hostname(hostname: String): Builder = {
      this.hostname = Objects.requireNonNull(hostname)
      this
    }

    /**
      * set remote's ssh port
      * @param port ssh port
      * @return this builder
      */
    def port(port: Int): Builder = {
      this.port = port
      this
    }

    /**
      * set remote user name
      * @param user user name
      * @return this builder
      */
    def user(user: String): Builder = {
      this.user = Objects.requireNonNull(user)
      this
    }

    /**
      * set remote's ssh password
      * @param password ssh password
      * @return this builder
      */
    def password(password: String): Builder = {
      this.password = Objects.requireNonNull(password)
      this
    }

    /**
      * set charset in communication
      * @param charset ssh charset
      * @return this builder
      */
    def charset(charset: Charset): Builder = {
      this.charset = Objects.requireNonNull(charset)
      this
    }

    def build(): Agent = new Agent {
      private[this] val hostname: String = Objects.requireNonNull(Builder.this.hostname)
      private[this] val port: Int = Builder.this.port
      private[this] val user: String = Objects.requireNonNull(Builder.this.user)
      private[this] val password: String = Objects.requireNonNull(Builder.this.password)
      private[this] val charset: Charset = Objects.requireNonNull(Builder.this.charset)
      private[this] val client = SshClient.setUpDefaultSimpleClient()
      override def execute(command: String): Option[String] = {
        // TODO: This method can't set the timeout...It is not ok in production I'd say...by chia
        val session = client.sessionLogin(hostname, port, user, password)
        try {
          val stdOut = new ByteArrayOutputStream
          try {
            val stdError = new ByteArrayOutputStream
            try {
              def response() = if (stdOut.size() != 0) Some(new String(stdOut.toByteArray, charset))
              else if (stdError.size() != 0) Some(new String(stdError.toByteArray, charset))
              else None
              try {
                session.executeRemoteCommand(command, stdOut, stdError, charset)
                response()
              } catch {
                case e: Throwable =>
                  LOG.error(response().getOrElse("no response"), e)
                  throw e
              }
            } finally stdError.close()
          } finally stdOut.close()
        } finally session.close()
      }
      override protected def doClose(): Unit = ReleaseOnce.close(client)
    }
  }
  private[this] val LOG = Logger(Agent.getClass)
}
