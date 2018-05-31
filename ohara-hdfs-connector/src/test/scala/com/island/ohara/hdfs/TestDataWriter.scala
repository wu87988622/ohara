package com.island.ohara.hdfs

import java.util

import com.island.ohara.hdfs.storage.HDFSStorage
import com.island.ohara.integration.{LocalHDFS, OharaTestUtil}
import com.island.ohara.io.CloseOnce.doClose
import com.island.ohara.kafka.connector.RowSinkRecord
import com.island.ohara.rule.MediumTest
import org.apache.hadoop.fs.FileSystem
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.connect.sink.SinkTaskContext
import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar

class TestDataWriter extends MediumTest with Matchers with MockitoSugar {

  @Test
  def testCreatePartitionDataWriters(): Unit = {
    val localHDFS: LocalHDFS = OharaTestUtil.localHDFS(1)

    val props: util.Map[String, String] = new util.HashMap[String, String]()
    props.put(HDFSSinkConnectorConfig.HDFS_URL, s"file://${localHDFS.tmpDirPath()}")
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig = new HDFSSinkConnectorConfig(props)
    val context: SinkTaskContext = mock[SinkTaskContext]
    val dataWriter: DataWriter = new DataWriter(hdfsSinkConnectorConfig, context)
    val topicName: String = "topic1"
    val listPartition =
      List(new TopicPartition(topicName, 0), new TopicPartition(topicName, 1), new TopicPartition(topicName, 2))

    dataWriter.createPartitionDataWriters(listPartition)
    dataWriter.topicPartitionWriters.size shouldBe 3
    dataWriter.removePartitionWriters(listPartition)
  }

  @Test
  def testWriterEmpty(): Unit = {
    val localHDFS: LocalHDFS = OharaTestUtil.localHDFS(1)

    val props: util.Map[String, String] = new util.HashMap[String, String]()
    props.put(HDFSSinkConnectorConfig.HDFS_URL, s"file://${localHDFS.tmpDirPath()}")
    val hdfsSinkConnectorConfig: HDFSSinkConnectorConfig = new HDFSSinkConnectorConfig(props)
    val context: SinkTaskContext = mock[SinkTaskContext]
    val dataWriter: DataWriter = new DataWriter(hdfsSinkConnectorConfig, context)

    val topicName: String = "topic1"
    val topicPartition1: TopicPartition = new TopicPartition(topicName, 0)
    val listPartition = List(topicPartition1)
    dataWriter.createPartitionDataWriters(listPartition)
    dataWriter.write(Array())
    dataWriter.topicPartitionWriters.size shouldBe 1
    dataWriter.topicPartitionWriters.foreach(topicPartitionWriter => {
      topicPartitionWriter._2.startTimeMS > 0 shouldBe true
    })
  }
}
