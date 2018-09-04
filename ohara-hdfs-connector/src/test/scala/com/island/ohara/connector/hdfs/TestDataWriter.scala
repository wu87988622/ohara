package com.island.ohara.connector.hdfs

import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.kafka.connector.{RowSinkContext, TopicPartition}
import com.island.ohara.rule.MediumTest
import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar

class TestDataWriter extends MediumTest with Matchers with MockitoSugar {

  @Test
  def testCreatePartitionDataWriters(): Unit = {
    val testUtil = OharaTestUtil.localHDFS(1)
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig = new HDFSSinkConnectorConfig(
      Map(HDFSSinkConnectorConfig.HDFS_URL -> s"file://${testUtil.tmpDirectory}"))
    val context: RowSinkContext = mock[RowSinkContext]
    val dataWriter: DataWriter = new DataWriter(hdfsSinkConnectorConfig, context)
    val topicName: String = "topic1"
    val listPartition =
      List(TopicPartition(topicName, 0), TopicPartition(topicName, 1), TopicPartition(topicName, 2))

    dataWriter.createPartitionDataWriters(listPartition)
    dataWriter.topicPartitionWriters.size shouldBe 3
    dataWriter.removePartitionWriters(listPartition)
  }

  @Test
  def testWriterEmpty(): Unit = {
    val testUtil = OharaTestUtil.localHDFS(1)
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig = new HDFSSinkConnectorConfig(
      Map(HDFSSinkConnectorConfig.HDFS_URL -> s"file://${testUtil.tmpDirectory}"))
    val context: RowSinkContext = mock[RowSinkContext]
    val dataWriter: DataWriter = new DataWriter(hdfsSinkConnectorConfig, context)

    val topicName: String = "topic1"
    val topicPartition1: TopicPartition = TopicPartition(topicName, 0)
    val listPartition = List(topicPartition1)
    dataWriter.createPartitionDataWriters(listPartition)
    dataWriter.write(Seq.empty)
    dataWriter.topicPartitionWriters.size shouldBe 1
    dataWriter.topicPartitionWriters.foreach(topicPartitionWriter => {
      topicPartitionWriter._2.startTimeMS > 0 shouldBe true
    })
  }
}
