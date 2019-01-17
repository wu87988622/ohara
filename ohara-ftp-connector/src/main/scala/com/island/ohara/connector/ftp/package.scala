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

package com.island.ohara.connector

package object ftp {
  val FTP_INPUT = "ftp.input.folder"
  val FTP_COMPLETED_FOLDER = "ftp.completed.folder"
  val FTP_OUTPUT = "ftp.output.folder"
  val FTP_ERROR = "ftp.error.folder"
  val FTP_HASH = "ftp.task.hash"
  val FTP_TOTAL = "ftp.task.count"
  val FTP_HOSTNAME = "ftp.hostname"
  val FTP_PORT = "ftp.port"
  val FTP_USER_NAME = "ftp.user.name"
  val FTP_PASSWORD = "ftp.user.password"
  val FTP_ENCODE = "ftp.encode"
  val FTP_NEEDHEADER = "ftp.needHeader"
}
