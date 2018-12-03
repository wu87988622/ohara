package com.island.ohara.connector.hdfs

import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.kafka.connector.{RowSinkContext, TopicPartition}
import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar

class TestDataWriter extends MediumTest with Matchers with MockitoSugar {

  @Test
  def testCreatePartitionDataWriters(): Unit = {
    val testUtil = OharaTestUtil.localHDFS()
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig = HDFSSinkConnectorConfig(
      Map(HDFS_URL -> s"${testUtil.hdfs.hdfsURL}"))
    val context: RowSinkContext = mock[RowSinkContext]
    val schema: Seq[Column] = Seq.empty
    val dataWriter: DataWriter = new DataWriter(hdfsSinkConnectorConfig, context, schema)
    val topicName: String = "topic1"
    val listPartition =
      List(new TopicPartition(topicName, 0), new TopicPartition(topicName, 1), new TopicPartition(topicName, 2))

    dataWriter.createPartitionDataWriters(listPartition)
    dataWriter.topicPartitionWriters.size shouldBe 3
    dataWriter.removePartitionWriters(listPartition)
  }

  @Test
  def testWriterEmpty(): Unit = {
    val testUtil = OharaTestUtil.localHDFS()
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig = HDFSSinkConnectorConfig(
      Map(HDFS_URL -> s"${testUtil.hdfs.hdfsURL}"))
    val context: RowSinkContext = mock[RowSinkContext]
    val schema: Seq[Column] = Seq.empty
    val dataWriter: DataWriter = new DataWriter(hdfsSinkConnectorConfig, context, schema)

    val topicName: String = "topic1"
    val topicPartition1: TopicPartition = new TopicPartition(topicName, 0)
    val listPartition = List(topicPartition1)
    dataWriter.createPartitionDataWriters(listPartition)
    dataWriter.write(Seq.empty)
    dataWriter.topicPartitionWriters.size shouldBe 1
    dataWriter.topicPartitionWriters.foreach(topicPartitionWriter => {
      topicPartitionWriter._2.startTimeMS > 0 shouldBe true
    })
  }
}
