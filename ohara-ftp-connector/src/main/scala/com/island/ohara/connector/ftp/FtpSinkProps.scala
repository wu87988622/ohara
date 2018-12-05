package com.island.ohara.connector.ftp

case class FtpSinkProps(output: String,
                        needHeader: Boolean,
                        encode: Option[String],
                        hostname: String,
                        port: Int,
                        user: String,
                        password: String) {
  def toMap: Map[String, String] = Map(
    FTP_OUTPUT -> output,
    FTP_NEEDHEADER -> needHeader.toString,
    FTP_ENCODE -> encode.getOrElse("null"),
    FTP_HOSTNAME -> hostname,
    FTP_PORT -> port.toString,
    FTP_USER_NAME -> user,
    FTP_PASSWORD -> password
  )
}

object FtpSinkProps {
  def apply(props: Map[String, String]): FtpSinkProps = FtpSinkProps(
    output = props(FTP_OUTPUT),
    needHeader = props(FTP_NEEDHEADER).toBoolean,
    encode = props.get(FTP_ENCODE).filter(_.toLowerCase != "null"),
    hostname = props(FTP_HOSTNAME),
    port = props(FTP_PORT).toInt,
    user = props(FTP_USER_NAME),
    password = props(FTP_PASSWORD)
  )
}
