package com.island.ohara.kafka.connector
import java.util

import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.rule.SmallTest
import com.island.ohara.serialization.DataType
import org.junit.Test
import org.scalatest.Matchers

class TestColumnInSource extends SmallTest with Matchers {

  private[this] val columns = Seq(Column("cf0", DataType.BOOLEAN, 0), Column("cf1", DataType.BOOLEAN, 1))
  @Test
  def testSource(): Unit = {
    val source = new RowSourceConnector {
      override protected def _taskClass(): Class[_ <: RowSourceTask] = ???
      override protected def _taskConfigs(maxTasks: Int): Seq[TaskConfig] = {
        Seq(TaskConfig("test", Seq("topic"), columns, Map(Column.COLUMN_KEY -> Column.toString(columns))))
      }
      override protected def _start(config: TaskConfig): Unit = ???
      override protected def _stop(): Unit = ???

      override protected def _version: String = ???
    }
    // lack column string
    an[IllegalArgumentException] should be thrownBy source.start(new util.HashMap[String, String])
    // It is invalid to assign the columns manually
    an[IllegalArgumentException] should be thrownBy source.taskConfigs(3)
  }

  @Test
  def testTask(): Unit = {
    val task = new RowSourceTask {

      override protected def _start(config: TaskConfig): Unit = ???

      override protected def _stop(): Unit = ???

      override protected def _poll(): Seq[RowSourceRecord] = ???

      override protected def _version: String = ???
    }

    an[IllegalArgumentException] should be thrownBy task.start(new util.HashMap[String, String])
  }
}
