package com.island.ohara.kafka.connector
import java.util

import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.rule.SmallTest
import com.island.ohara.serialization.DataType
import com.island.ohara.util.VersionUtil
import org.junit.Test
import org.scalatest.Matchers

class TestConnectorProps extends SmallTest with Matchers {

  @Test
  def testNoTopicsInSourceConnector(): Unit = {
    val connector = new DumbSource
    // lack topics string
    an[IllegalArgumentException] should be thrownBy connector.start(new util.HashMap[String, String])
  }

  @Test
  def testNoTopicsInSinkConnector(): Unit = {
    val connector = new DumbSink
    // lack topics string
    an[IllegalArgumentException] should be thrownBy connector.start(new util.HashMap[String, String])
  }

  @Test
  def testNoTopicsInSourceTask(): Unit = {
    val task = new DumbSourceTask
    an[IllegalArgumentException] should be thrownBy task.start(new util.HashMap[String, String])
  }

  @Test
  def testNoTopicsInSinkTask(): Unit = {
    val task = new DumbSinkTask
    an[IllegalArgumentException] should be thrownBy task.start(new util.HashMap[String, String])
  }

  // TODO: add tests against adding interval key manually...see OHARA-588
}

class DumbSource extends RowSourceConnector {
  private[this] val columns = Seq(Column("cf0", DataType.BOOLEAN, 0), Column("cf1", DataType.BOOLEAN, 1))
  override protected def _taskClass(): Class[_ <: RowSourceTask] = classOf[DumbSourceTask]
  override protected def _taskConfigs(maxTasks: Int): Seq[TaskConfig] = {
    Seq(TaskConfig("test", Seq("topic"), columns, Map(Column.COLUMN_KEY -> Column.toString(columns))))
  }
  override protected def _start(config: TaskConfig): Unit = {}
  override protected def _stop(): Unit = {}

  override protected def _version: String = VersionUtil.VERSION
}

class DumbSourceTask extends RowSourceTask {
  override protected def _start(config: TaskConfig): Unit = {}

  override protected def _stop(): Unit = {}

  override protected def _poll(): Seq[RowSourceRecord] = Seq.empty

  override protected def _version: String = VersionUtil.VERSION
}

class DumbSink extends RowSinkConnector {
  private[this] val columns = Seq(Column("cf0", DataType.BOOLEAN, 0), Column("cf1", DataType.BOOLEAN, 1))
  override protected def _taskClass(): Class[_ <: RowSinkTask] = classOf[DumbSinkTask]
  override protected def _taskConfigs(maxTasks: Int): Seq[TaskConfig] = {
    Seq(TaskConfig("test", Seq("topic"), columns, Map(Column.COLUMN_KEY -> Column.toString(columns))))
  }
  override protected def _start(config: TaskConfig): Unit = {}
  override protected def _stop(): Unit = {}

  override protected def _version: String = VersionUtil.VERSION
}

class DumbSinkTask extends RowSinkTask {
  override protected def _start(config: TaskConfig): Unit = {}

  override protected def _stop(): Unit = {}

  override protected def _version: String = VersionUtil.VERSION

  override protected def _put(records: Seq[RowSinkRecord]): Unit = {}
}
