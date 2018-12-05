package com.island.ohara.connector.ftp

case class FtpSourceProps(inputFolder: String,
                          completedFolder: Option[String],
                          errorFolder: String,
                          encode: Option[String],
                          hostname: String,
                          port: Int,
                          user: String,
                          password: String) {
  def toMap: Map[String, String] = Map(
    FTP_INPUT -> inputFolder,
    FTP_COMPLETED_FOLDER -> completedFolder.getOrElse("null"),
    FTP_ERROR -> errorFolder,
    FTP_ENCODE -> encode.getOrElse("null"),
    FTP_HOSTNAME -> hostname,
    FTP_PORT -> port.toString,
    FTP_USER_NAME -> user,
    FTP_PASSWORD -> password
  )
}

object FtpSourceProps {
  def apply(inputFolder: String,
            completedFolder: String,
            errorFolder: String,
            encode: Option[String],
            hostname: String,
            port: Int,
            user: String,
            password: String): FtpSourceProps = FtpSourceProps(
    inputFolder = inputFolder,
    completedFolder = Some(completedFolder),
    errorFolder = errorFolder,
    encode = encode,
    hostname = hostname,
    port = port,
    user = user,
    password = password
  )
  def apply(props: Map[String, String]): FtpSourceProps = FtpSourceProps(
    inputFolder = props(FTP_INPUT),
    completedFolder = props.get(FTP_COMPLETED_FOLDER).filter(_.toLowerCase != "null"),
    errorFolder = props(FTP_ERROR),
    encode = props.get(FTP_ENCODE).filter(_.toLowerCase != "null"),
    hostname = props(FTP_HOSTNAME),
    port = props(FTP_PORT).toInt,
    user = props(FTP_USER_NAME),
    password = props(FTP_PASSWORD)
  )
}
