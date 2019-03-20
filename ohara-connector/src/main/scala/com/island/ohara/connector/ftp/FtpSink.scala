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

import java.util

import com.island.ohara.client.ftp.FtpClient
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector._

import scala.collection.JavaConverters._

class FtpSink extends RowSinkConnector {
  private[this] var config: TaskConfig = _
  private[this] var props: FtpSinkProps = _

  override protected[ftp] def _start(config: TaskConfig): Unit = {
    this.config = config
    this.props = FtpSinkProps(config.raw().asScala.toMap)
    if (config.columns.asScala.exists(_.order == 0))
      throw new IllegalArgumentException("column order must be bigger than zero")

    val ftpClient =
      FtpClient.builder().hostname(props.hostname).port(props.port).user(props.user).password(props.password).build()
    try if (!ftpClient.exist(props.output)) ftpClient.mkdir(props.output)
    finally ftpClient.close()
  }

  override protected def _stop(): Unit = {
    // do nothing
  }

  override protected def _taskClass(): Class[_ <: RowSinkTask] = classOf[FtpSinkTask]

  override protected def _taskConfigs(maxTasks: Int): util.List[TaskConfig] = {
    (0 until maxTasks)
      .map(
        index =>
          config.append(FtpSinkTaskProps(
            output = CommonUtils.path(props.output, s"${config.name}_$index"),
            needHeader = props.needHeader,
            encode = props.encode,
            hostname = props.hostname,
            port = props.port,
            user = props.user,
            password = props.password
          ).toMap.asJava))
      .asJava
  }
}
