package com.island.ohara.connector.ftp

case class FtpSinkProps(output: String,
                        header: Boolean,
                        encode: Option[String],
                        host: String,
                        port: Int,
                        user: String,
                        password: String) {
  def toMap: Map[String, String] = Map(
    FTP_OUTPUT -> output,
    FTP_HEADER -> header.toString,
    FTP_ENCODE -> encode.getOrElse("null"),
    FTP_HOST -> host,
    FTP_PORT -> port.toString,
    FTP_USER_NAME -> user,
    FTP_PASSWORD -> password
  )
}

object FtpSinkProps {
  def apply(props: Map[String, String]): FtpSinkProps = FtpSinkProps(
    output = props(FTP_OUTPUT),
    header = props(FTP_HEADER).toBoolean,
    encode = props.get(FTP_ENCODE).filter(_.toLowerCase != "null"),
    host = props(FTP_HOST),
    port = props(FTP_PORT).toInt,
    user = props(FTP_USER_NAME),
    password = props(FTP_PASSWORD)
  )
}
