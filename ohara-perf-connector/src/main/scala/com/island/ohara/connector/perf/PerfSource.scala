package com.island.ohara.connector.perf
import com.island.ohara.kafka.connector.{RowSourceConnector, RowSourceTask, TaskConfig}

import scala.collection.JavaConverters._

class PerfSource extends RowSourceConnector {
  private[this] var config: TaskConfig = _

  override protected def _taskClass(): Class[_ <: RowSourceTask] = classOf[PerfSourceTask]

  override protected def _taskConfigs(maxTasks: Int): java.util.List[TaskConfig] = Seq.fill(maxTasks)(config).asJava

  /**
    * this method is exposed to test scope
    */
  override protected[perf] def _start(config: TaskConfig): Unit = {
    if (config.schema.isEmpty) throw new IllegalArgumentException("schema can't be empty")
    if (config.topics.isEmpty) throw new IllegalArgumentException("topics can't be empty")
    val props = PerfSourceProps(config.options().asScala.toMap)
    if (props.batch < 0) throw new IllegalArgumentException(s"batch:${props.batch} can't be negative")
    this.config = config
  }

  override protected def _stop(): Unit = {}
}
