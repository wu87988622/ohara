package com.island.ohara.connector.ftp

import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.client.FtpClient
import com.island.ohara.io.VersionUtil
import com.island.ohara.kafka.connector.{RowSourceConnector, RowSourceTask, TaskConfig}
import org.slf4j.{Logger, LoggerFactory}

class FtpSource extends RowSourceConnector {
  private[this] var config: TaskConfig = _
  private[this] var props: FtpSourceProps = _
  private[this] var schema: Seq[Column] = _

  override protected def _taskClass(): Class[_ <: RowSourceTask] = classOf[FtpSourceTask]
  override protected def _taskConfigs(maxTasks: Int): Seq[TaskConfig] = {
    (0 until maxTasks).map(
      index =>
        TaskConfig(
          config.name,
          config.topics,
          schema,
          FtpSourceTaskProps(
            total = maxTasks,
            hash = index,
            input = props.input,
            output = props.output,
            error = props.error,
            encode = props.encode,
            host = props.host,
            port = props.port,
            user = props.user,
            password = props.password
          ).toMap
      ))
  }

  override protected def _start(config: TaskConfig): Unit = {
    this.config = config
    this.props = FtpSourceProps(config.options)
    this.schema = config.schema
    if (schema.exists(_.order == 0)) throw new IllegalArgumentException("column order must be bigger than zero")

    val ftpClient =
      FtpClient.builder().host(props.host).port(props.port).user(props.user).password(props.password).build()
    try {
      def check(path: String, prefix: String): Unit =
        if (!ftpClient.exist(path)) throw new IllegalArgumentException(s"$prefix:$path doesn't exist")
      check(props.input, "input")
      check(props.error, "error")
      check(props.output, "output")
    } finally ftpClient.close()
  }

  override protected def _stop(): Unit = {
    // do nothing
  }

  override protected def _version: String = VersionUtil.VERSION
}

object FtpSource {
  val LOG: Logger = LoggerFactory.getLogger(classOf[FtpSource])
}
