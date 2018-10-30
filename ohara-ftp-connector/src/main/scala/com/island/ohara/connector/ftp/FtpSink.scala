package com.island.ohara.connector.ftp

import com.island.ohara.client.FtpClient
import com.island.ohara.io.IoUtil
import com.island.ohara.kafka.connector._
import com.island.ohara.util.VersionUtil

class FtpSink extends RowSinkConnector {
  private[this] var config: TaskConfig = _
  private[this] var props: FtpSinkProps = _

  override protected def _taskClass(): Class[_ <: RowSinkTask] = classOf[FtpSinkTask]
  override protected def _taskConfigs(maxTasks: Int): Seq[TaskConfig] = {
    (0 until maxTasks).map(
      index =>
        TaskConfig(
          config.name,
          config.topics,
          config.schema,
          FtpSinkTaskProps(
            output = IoUtil.path(props.output, s"${config.name}_$index"),
            needHeader = props.needHeader,
            encode = props.encode,
            host = props.host,
            port = props.port,
            user = props.user,
            password = props.password
          ).toMap
      ))
  }

  override protected[ftp] def _start(config: TaskConfig): Unit = {
    this.config = config
    this.props = FtpSinkProps(config.options)
    if (config.schema.exists(_.order == 0)) throw new IllegalArgumentException("column order must be bigger than zero")

    val ftpClient =
      FtpClient.builder().host(props.host).port(props.port).user(props.user).password(props.password).build()
    try if (!ftpClient.exist(props.output)) ftpClient.mkdir(props.output)
    finally ftpClient.close()
  }

  override protected def _stop(): Unit = {
    // do nothing
  }

  override protected def _version: String = VersionUtil.VERSION
}
