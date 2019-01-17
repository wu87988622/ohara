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

package com.island.ohara.client
import java.io.File

import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.server.directives.FileInfo
import com.island.ohara.common.util.VersionUtil

trait StreamClient extends AutoCloseable {

  def saveFile(fileInfo: FileInfo): File

}

object StreamClient {

  /**
    * StreamApp List Page max acceptable upload file size (1 MB currently)
    */
  final val MAX_FILE_SIZE = 1 * 1024 * 1024L

  /**
    * StreamApp List Page "key name" for form-data
    */
  final val INPUT_KEY = "streamapp"

  final val CONTENT_TYPE = MediaTypes.`application/java-archive`

  final val TMP_ROOT = System.getProperty("java.io.tmpdir")

  /**
    * StreamApp Docker Image name
    */
  final val STREAMAPP_IMAGE: String = s"oharastream/streamapp:${VersionUtil.VERSION}"

  /**
    * Save http request files by [[File.createTempFile]]
    *
    * @param fileInfo the request file
    * @return the tmp file
    */
  def saveTmpFile(fileInfo: FileInfo): File = File.createTempFile(fileInfo.fileName, ".tmp")
}
