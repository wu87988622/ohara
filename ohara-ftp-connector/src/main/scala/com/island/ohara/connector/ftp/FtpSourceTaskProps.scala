package com.island.ohara.connector.ftp

case class FtpSourceTaskProps(hash: Int,
                              total: Int,
                              input: String,
                              output: String,
                              error: String,
                              encode: Option[String],
                              host: String,
                              port: Int,
                              user: String,
                              password: String) {
  def toMap: Map[String, String] = Map(
    FTP_HASH -> hash.toString,
    FTP_TOTAL -> total.toString,
    FTP_INPUT -> input,
    FTP_OUTPUT -> output,
    FTP_ERROR -> error,
    FTP_ENCODE -> encode.getOrElse("null"),
    FTP_HOST -> host,
    FTP_PORT -> port.toString,
    FTP_USER_NAME -> user,
    FTP_PASSWORD -> password
  )
}

object FtpSourceTaskProps {
  def apply(props: Map[String, String]): FtpSourceTaskProps = FtpSourceTaskProps(
    hash = props(FTP_HASH).toInt,
    total = props(FTP_TOTAL).toInt,
    input = props(FTP_INPUT),
    output = props(FTP_OUTPUT),
    error = props(FTP_ERROR),
    encode = props.get(FTP_ENCODE).filter(_.toLowerCase != "null"),
    host = props(FTP_HOST),
    port = props(FTP_PORT).toInt,
    user = props(FTP_USER_NAME),
    password = props(FTP_PASSWORD),
  )
}
