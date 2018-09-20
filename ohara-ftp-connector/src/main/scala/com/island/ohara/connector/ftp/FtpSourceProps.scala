package com.island.ohara.connector.ftp

case class FtpSourceProps(input: String,
                          output: String,
                          error: String,
                          encode: Option[String],
                          host: String,
                          port: Int,
                          user: String,
                          password: String) {
  def toMap: Map[String, String] = Map(
    FTP_INPUT -> input,
    FTP_COMPLETED_FOLDER -> output,
    FTP_ERROR -> error,
    FTP_ENCODE -> encode.getOrElse("null"),
    FTP_HOST -> host,
    FTP_PORT -> port.toString,
    FTP_USER_NAME -> user,
    FTP_PASSWORD -> password
  )
}

object FtpSourceProps {
  def apply(props: Map[String, String]): FtpSourceProps = FtpSourceProps(
    input = props(FTP_INPUT),
    output = props(FTP_COMPLETED_FOLDER),
    error = props(FTP_ERROR),
    encode = props.get(FTP_ENCODE).filter(_.toLowerCase != "null"),
    host = props(FTP_HOST),
    port = props(FTP_PORT).toInt,
    user = props(FTP_USER_NAME),
    password = props(FTP_PASSWORD)
  )
}
