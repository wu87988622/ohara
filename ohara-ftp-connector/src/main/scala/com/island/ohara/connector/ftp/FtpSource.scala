package com.island.ohara.connector.ftp

import java.util

import com.island.ohara.client.FtpClient
import com.island.ohara.common.data.Column
import com.island.ohara.kafka.connector.{RowSourceConnector, RowSourceTask, TaskConfig}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

class FtpSource extends RowSourceConnector {
  private[this] var config: TaskConfig = _
  private[this] var props: FtpSourceProps = _
  private[this] var schema: Seq[Column] = _

  override protected def _taskClass(): Class[_ <: RowSourceTask] = classOf[FtpSourceTask]

  override protected def _taskConfigs(maxTasks: Int): util.List[TaskConfig] = {
    (0 until maxTasks)
      .map(
        index =>
          TaskConfig
            .builder()
            .name(config.name)
            .topics(config.topics)
            .schema(schema.asJava)
            .options(FtpSourceTaskProps(
              total = maxTasks,
              hash = index,
              inputFolder = props.inputFolder,
              completedFolder = props.completedFolder,
              errorFolder = props.errorFolder,
              encode = props.encode,
              hostname = props.hostname,
              port = props.port,
              user = props.user,
              password = props.password
            ).toMap.asJava)
            .build()
      )
      .asJava
  }

  override protected[ftp] def _start(config: TaskConfig): Unit = {
    this.config = config
    this.props = FtpSourceProps(config.options.asScala.toMap)
    this.schema = config.schema.asScala
    if (schema.exists(_.order == 0)) throw new IllegalArgumentException("column order must be bigger than zero")

    val ftpClient =
      FtpClient.builder().hostname(props.hostname).port(props.port).user(props.user).password(props.password).build()
    try {
      if (ftpClient.nonExist(props.inputFolder))
        throw new IllegalArgumentException(s"${props.inputFolder} doesn't exist")
      if (ftpClient.nonExist(props.errorFolder)) ftpClient.mkdir(props.errorFolder)
      props.completedFolder.foreach(folder => if (ftpClient.nonExist(folder)) ftpClient.mkdir(folder))
    } finally ftpClient.close()
  }

  override protected def _stop(): Unit = {
    //    do nothing
  }
}

object FtpSource {
  val LOG: Logger = LoggerFactory.getLogger(classOf[FtpSource])
}
