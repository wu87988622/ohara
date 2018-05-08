package com.island.ohara.kafka.connector

import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.CloseOnce._
import com.island.ohara.rule.MediumTest
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestRowConnector extends MediumTest with Matchers {

  @Test
  def testCreateConnectorWithMultiWorkers(): Unit = {
    val sourceTasks = 3
    val sinkTasks = 2
    doClose(new OharaTestUtil(3, 2)) { testUtil =>
      {
        testUtil.availableConnectors().contains(classOf[SimpleRowSourceConnector].getSimpleName) shouldBe true
        testUtil.runningConnectors() shouldBe "[]"
        var resp = testUtil.startConnector(s"""{"name":"my_source_connector", "config":{"connector.class":"${classOf[
          SimpleRowSourceConnector].getName}","topic":"my_connector_topic","tasks.max":"$sourceTasks"}}""")
        withClue(s"body:${resp._2}") {
          resp._1 shouldBe 201
        }
        // wait for starting the source connector
        testUtil.await(() => testUtil.runningConnectors().contains("my_source_connector"), 10 second)
        // wait for starting the source task
        testUtil.await(() => SimpleRowSourceTask.taskCount.get >= sourceTasks, 10 second)
        resp = testUtil.startConnector(s"""{"name":"my_sink_connector", "config":{"connector.class":"${classOf[
          SimpleRowSinkConnector].getName}","topics":"my_connector_topic","tasks.max":"$sinkTasks"}}""")
        withClue(s"body:${resp._2}") {
          resp._1 shouldBe 201
        }
        // wait for starting the sink connector
        testUtil.await(() => testUtil.runningConnectors().contains("my_sink_connector"), 10 second)
        // wait for starting the sink task
        testUtil.await(() => SimpleRowSinkTask.taskCount.get >= sinkTasks, 10 second)

        // check the data sent by source task
        testUtil.await(() => SimpleRowSourceTask.taskValues.size == sourceTasks * SimpleRowSourceTask.dataSet.size,
                       30 second)
        SimpleRowSourceTask.dataSet.foreach(value => SimpleRowSourceTask.taskValues.contains(value) shouldBe true)

        // check the data received by sink task
        testUtil.await(() => SimpleRowSinkTask.taskValues.size == sourceTasks * SimpleRowSourceTask.dataSet.size,
                       30 second)
        SimpleRowSourceTask.dataSet.foreach(value => SimpleRowSinkTask.taskValues.contains(value) shouldBe true)
      }
    }
  }
}
