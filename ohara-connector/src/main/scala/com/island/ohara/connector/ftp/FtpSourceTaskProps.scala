/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.connector.ftp

import com.island.ohara.common.util.CommonUtils

case class FtpSourceTaskProps(hash: Int,
                              total: Int,
                              inputFolder: String,
                              completedFolder: Option[String],
                              errorFolder: String,
                              encode: String,
                              hostname: String,
                              port: Int,
                              user: String,
                              password: String) {
  def toMap: Map[String, String] = Map(
    FTP_HASH -> hash.toString,
    FTP_TOTAL -> total.toString,
    FTP_INPUT -> inputFolder,
    FTP_COMPLETED_FOLDER -> completedFolder.getOrElse(""),
    FTP_ERROR -> errorFolder,
    FTP_ENCODE -> encode,
    FTP_HOSTNAME -> hostname,
    FTP_PORT -> port.toString,
    FTP_USER_NAME -> user,
    FTP_PASSWORD -> password
  ).filter(_._2.nonEmpty)
}

object FtpSourceTaskProps {
  def apply(hash: Int,
            total: Int,
            inputFolder: String,
            completedFolder: String,
            errorFolder: String,
            encode: String,
            hostname: String,
            port: Int,
            user: String,
            password: String): FtpSourceTaskProps = FtpSourceTaskProps(
    hash = hash,
    total = total,
    inputFolder = inputFolder,
    completedFolder = Some(completedFolder),
    errorFolder = errorFolder,
    encode = encode,
    hostname = hostname,
    port = port,
    user = user,
    password = password
  )
  def apply(props: Map[String, String]): FtpSourceTaskProps = FtpSourceTaskProps(
    hash = props(FTP_HASH).toInt,
    total = props(FTP_TOTAL).toInt,
    inputFolder = props(FTP_INPUT),
    completedFolder = props.get(FTP_COMPLETED_FOLDER).filterNot(CommonUtils.isEmpty),
    errorFolder = props(FTP_ERROR),
    encode = props(FTP_ENCODE),
    hostname = props(FTP_HOSTNAME),
    port = props(FTP_PORT).toInt,
    user = props(FTP_USER_NAME),
    password = props(FTP_PASSWORD)
  )
}
