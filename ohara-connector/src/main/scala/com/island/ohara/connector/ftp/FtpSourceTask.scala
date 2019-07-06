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

import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.Collections
import java.util

import com.island.ohara.client.ftp.FtpClient
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.connector.ftp.FtpSource.LOG
import com.island.ohara.kafka.connector._
import com.island.ohara.kafka.connector.text.{TextFileSystem, TextSourceConverterFactory, TextSourceTask}
import TextSourceConverterFactory.TextType

import scala.collection.JavaConverters._

/**
  * Move files from FTP server to Kafka topics. The file format must be csv file, and element in same line must be separated
  * by comma. The offset is (path, line index). It means each line is stored as a "message" in connector topic. For example:
  * a file having 100 lines has 100 message in connector topic. If the file is processed correctly, the TestFtpSource
  */
class FtpSourceTask extends TextSourceTask {

  override def getConverterFactory(config: TaskSetting): TextSourceConverterFactory =
    TextSourceConverterFactory.of(config, TextType.CSV)

  override def getFileSystem(config: TaskSetting): TextFileSystem = new TextFileSystem {
    private[this] val props: FtpSourceTaskProps = FtpSourceTaskProps(config)
    private[this] val ftpClient: FtpClient =
      FtpClient.builder().hostname(props.hostname).port(props.port).user(props.user).password(props.password).build()

    if (props.inputFolder.isEmpty)
      throw new IllegalArgumentException(s"invalid input:${props.inputFolder.mkString(",")}")

    override def listInputFiles(): util.Collection[String] = try ftpClient
      .listFileNames(props.inputFolder)
      .map(CommonUtils.path(props.inputFolder, _))
      .filter(_.hashCode % props.total == props.hash)
      .asJava
    catch {
      case e: Throwable =>
        LOG.error(s"failed to list ${props.inputFolder}", e)
        Collections.emptyList()
    }

    override def createReader(path: String): InputStreamReader =
      new InputStreamReader(ftpClient.open(path), Charset.forName(props.encode))

    override def handleErrorFile(path: String): Unit = try {
      val outputPath = CommonUtils.replaceParent(props.errorFolder, path)
      if (ftpClient.exist(outputPath)) {
        val newPath = outputPath + s".${CommonUtils.uuid()}"
        if (ftpClient.exist(newPath)) throw new IllegalStateException(s"duplicate file $path??")
        else ftpClient.moveFile(path, newPath)
      } else ftpClient.moveFile(path, outputPath)
    } catch {
      case e: Throwable => LOG.error(s"failed to move $path to ${props.errorFolder}", e)
    }

    override def handleCompletedFile(path: String): Unit =
      props.completedFolder
        .fold(() => ftpClient.delete(path))(folder =>
          () => {
            try {
              val outputPath = CommonUtils.replaceParent(folder, path)
              if (ftpClient.exist(outputPath)) {
                val newPath = outputPath + s".${CommonUtils.uuid()}"
                if (ftpClient.exist(newPath)) throw new IllegalStateException(s"duplicate file $path??")
                else ftpClient.moveFile(path, newPath)
              } else ftpClient.moveFile(path, outputPath)
            } catch {
              case e: Throwable =>
                if (props.completedFolder.isDefined)
                  LOG.error(s"failed to move $path to ${props.completedFolder.get}", e)
                else LOG.error(s"failed to remove $path", e)
            }
        })
        .apply()

    override def close(): Unit = {
      Releasable.close(ftpClient)
    }
  }
}
