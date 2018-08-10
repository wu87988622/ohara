package com.island.ohara.integration
import com.island.ohara.io.ByteUtil
import com.island.ohara.io.CloseOnce.doClose
import com.island.ohara.client.ConnectorJson.ConnectorResponse
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer}
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestMiniKafka extends With3Blockers3Workers with Matchers {

  @Test
  def testCreateClusterWithMultiBrokers(): Unit = {
    testUtil.kafkaBrokers.size shouldBe 3
    testUtil.createTopic("my_topic")
    testUtil.exist("my_topic") shouldBe true
    val (_, valueQueue) = testUtil.run("my_topic", true, new ByteArrayDeserializer, new ByteArrayDeserializer)
    val totalMessageCount = 100
    doClose(
      new KafkaProducer[Array[Byte], Array[Byte]](testUtil.producerConfig.toProperties,
                                                  new ByteArraySerializer,
                                                  new ByteArraySerializer)) { producer =>
      {
        var count: Int = totalMessageCount
        while (count > 0) {
          producer.send(
            new ProducerRecord[Array[Byte], Array[Byte]]("my_topic",
                                                         ByteUtil.toBytes("key"),
                                                         ByteUtil.toBytes("value")))
          count -= 1
        }
      }
    }
    OharaTestUtil.await(() => valueQueue.size() == totalMessageCount, 1 minute)
    valueQueue.forEach((value: Array[Byte]) => ByteUtil.toString(value) shouldBe "value")
  }

  @Test
  def testCreateConnectorWithMultiWorkers(): Unit = {
    val sourceTasks = 3
    val sinkTasks = 2
    doClose(testUtil.connectorClient()) { connectorClient =>
      {
        connectorClient
          .plugins()
          .filter(_.className.equals(classOf[SimpleSourceConnector].getName))
          .isEmpty shouldBe false
        connectorClient.activeConnectors().size shouldBe 0
        val sourceConnectorName = "my_source_connector"
        val topicName = "my_connector_topic"
        var resp: ConnectorResponse = connectorClient
          .sourceConnectorCreator()
          .name(sourceConnectorName)
          .connectorClass(classOf[SimpleSourceConnector])
          .topic(topicName)
          .taskNumber(sourceTasks)
          .config("key0", "value0")
          .build()
        resp.name shouldBe sourceConnectorName
        // TODO: enable following check after KAFKA-7253 is resolved. by chia
        // resp.typeName shouldBe "source"
        resp.config.get("key0").get shouldBe "value0"
        // wait for starting the source connector
        OharaTestUtil.await(() => connectorClient.activeConnectors().size == 1, 10 second)
        // wait for starting the source task
        OharaTestUtil.await(() => SimpleSourceTask.taskCount.get >= sourceTasks, 10 second)
        val sinkConnectorName = "my_sink_connector"
        resp = connectorClient
          .sinkConnectorCreator()
          .name(sinkConnectorName)
          .connectorClass(classOf[SimpleSinkConnector])
          .topic(topicName)
          .taskNumber(sinkTasks)
          .config("key0", "value0")
          .build()

        resp.name shouldBe sinkConnectorName
        // TODO: enable following check after KAFKA-7253 is resolved. by chia
        // resp.typeName shouldBe "sink"
        resp.config.get("key0").get shouldBe "value0"
        // wait for starting the sink connector
        OharaTestUtil.await(() => connectorClient.activeConnectors().size == 2, 10 second)
        // wait for starting the sink task
        OharaTestUtil.await(() => SimpleSinkTask.taskCount.get >= sinkTasks, 10 second)

        // check the data sent by source task
        OharaTestUtil.await(() => SimpleSourceTask.taskValues.size == sourceTasks * SimpleSourceTask.dataSet.size,
                            30 second)
        SimpleSourceTask.dataSet.foreach(value => SimpleSourceTask.taskValues.contains(value) shouldBe true)

        // check the data received by sink task
        OharaTestUtil.await(() => SimpleSinkTask.taskValues.size == sourceTasks * SimpleSourceTask.dataSet.size,
                            30 second)
        SimpleSourceTask.dataSet.foreach(value => SimpleSinkTask.taskValues.contains(value) shouldBe true)
      }
    }

  }
}
