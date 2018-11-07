package com.island.ohara.connector.ftp

case class FtpSourceTaskProps(hash: Int,
                              total: Int,
                              inputFolder: String,
                              completedFolder: Option[String],
                              errorFolder: String,
                              encode: Option[String],
                              host: String,
                              port: Int,
                              user: String,
                              password: String) {
  def toMap: Map[String, String] = Map(
    FTP_HASH -> hash.toString,
    FTP_TOTAL -> total.toString,
    FTP_INPUT -> inputFolder,
    FTP_COMPLETED_FOLDER -> completedFolder.getOrElse("null"),
    FTP_ERROR -> errorFolder,
    FTP_ENCODE -> encode.getOrElse("null"),
    FTP_HOST -> host,
    FTP_PORT -> port.toString,
    FTP_USER_NAME -> user,
    FTP_PASSWORD -> password
  )
}

object FtpSourceTaskProps {
  def apply(hash: Int,
            total: Int,
            inputFolder: String,
            completedFolder: String,
            errorFolder: String,
            encode: Option[String],
            host: String,
            port: Int,
            user: String,
            password: String): FtpSourceTaskProps = FtpSourceTaskProps(
    hash = hash,
    total = total,
    inputFolder = inputFolder,
    completedFolder = Some(completedFolder),
    errorFolder = errorFolder,
    encode = encode,
    host = host,
    port = port,
    user = user,
    password = password
  )
  def apply(props: Map[String, String]): FtpSourceTaskProps = FtpSourceTaskProps(
    hash = props(FTP_HASH).toInt,
    total = props(FTP_TOTAL).toInt,
    inputFolder = props(FTP_INPUT),
    completedFolder = props.get(FTP_COMPLETED_FOLDER).filter(_.toLowerCase != "null"),
    errorFolder = props(FTP_ERROR),
    encode = props.get(FTP_ENCODE).filter(_.toLowerCase != "null"),
    host = props(FTP_HOST),
    port = props(FTP_PORT).toInt,
    user = props(FTP_USER_NAME),
    password = props(FTP_PASSWORD)
  )
}
