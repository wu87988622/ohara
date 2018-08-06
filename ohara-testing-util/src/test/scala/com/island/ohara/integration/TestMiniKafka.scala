package com.island.ohara.integration
import com.island.ohara.io.ByteUtil
import com.island.ohara.io.CloseOnce.doClose
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
    testUtil.availableConnectors().body.contains(classOf[SimpleSourceConnector].getSimpleName) shouldBe true
    testUtil.runningConnectors().body shouldBe "[]"
    var resp = testUtil
      .sourceConnectorCreator()
      .name("my_source_connector")
      .connectorClass(classOf[SimpleSourceConnector])
      .topic("my_connector_topic")
      .taskNumber(sourceTasks)
      .run()
    withClue(s"body:${resp.body}") {
      resp.statusCode shouldBe 201
    }
    // wait for starting the source connector
    OharaTestUtil.await(() => testUtil.runningConnectors().body.contains("my_source_connector"), 10 second)
    // wait for starting the source task
    OharaTestUtil.await(() => SimpleSourceTask.taskCount.get >= sourceTasks, 10 second)
    resp = testUtil
      .sinkConnectorCreator()
      .name("my_sink_connector")
      .connectorClass(classOf[SimpleSinkConnector])
      .topic("my_connector_topic")
      .taskNumber(sinkTasks)
      .run()
    withClue(s"body:${resp.body}") {
      resp.statusCode shouldBe 201
    }
    // wait for starting the sink connector
    OharaTestUtil.await(() => testUtil.runningConnectors().body.contains("my_sink_connector"), 10 second)
    // wait for starting the sink task
    OharaTestUtil.await(() => SimpleSinkTask.taskCount.get >= sinkTasks, 10 second)

    // check the data sent by source task
    OharaTestUtil
      .await(() => SimpleSourceTask.taskValues.size == sourceTasks * SimpleSourceTask.dataSet.size, 30 second)
    SimpleSourceTask.dataSet.foreach(value => SimpleSourceTask.taskValues.contains(value) shouldBe true)

    // check the data received by sink task
    OharaTestUtil.await(() => SimpleSinkTask.taskValues.size == sourceTasks * SimpleSourceTask.dataSet.size, 30 second)
    SimpleSourceTask.dataSet.foreach(value => SimpleSinkTask.taskValues.contains(value) shouldBe true)
  }
}
