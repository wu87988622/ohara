/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.connector.hdfs

import java.io.OutputStream

import com.island.ohara.common.data.{Cell, Column, Row}
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.connector.hdfs.storage.{HDFSStorage, Storage}
import com.island.ohara.connector.hdfs.text.RecordWriterOutput
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.kafka.connector.{RowSinkContext, RowSinkRecord, TopicPartition}
import org.junit.Test
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar

class TestTopicPartitionWriter extends MediumTest with Matchers with MockitoSugar {

  @Test
  def testOpenTempFile(): Unit = {
    val hdfsSinkConnectorConfig = HDFSSinkConnectorConfig(Map(HDFS_URL -> "", TMP_DIR -> "/tmp"))
    val sinkTaskContext = mock[RowSinkContext]
    val topicPartition = new TopicPartition("topic1", 0)
    val testUtil = OharaTestUtil.localHDFS()
    val storage = new HDFSStorage(testUtil.hdfs.fileSystem())
    val topicPartitionWriter =
      new TopicPartitionWriter(hdfsSinkConnectorConfig, sinkTaskContext, topicPartition, storage)
    new TopicPartitionWriter(hdfsSinkConnectorConfig, sinkTaskContext, topicPartition, storage)

    topicPartitionWriter.openTempFile(0)
    topicPartitionWriter.processLineCount shouldBe 0
    storage.exists(topicPartitionWriter.tmpFilePath) shouldBe false
  }

  @Test
  def testWriteData(): Unit = {
    val hdfsSinkConnectorConfig = HDFSSinkConnectorConfig(Map(HDFS_URL -> "", TMP_DIR -> "/tmp"))

    val sinkTaskContext = mock[RowSinkContext]
    val topicPartition = new TopicPartition("topic1", 0)
    val storage = mock[Storage]
    when(storage.open(anyString(), anyBoolean())).thenReturn(mock[OutputStream])

    val recordWriterOutput = mock[RecordWriterOutput]
    val topicPartitionWriter =
      new TopicPartitionWriter(hdfsSinkConnectorConfig, sinkTaskContext, topicPartition, storage)

    val rowSinkRecord = mock[RowSinkRecord]
    when(rowSinkRecord.row).thenReturn(Row.of(Cell.of("column1", "value")))

    val schema: Seq[Column] = Seq()
    topicPartitionWriter.recordWriterOutput = recordWriterOutput
    topicPartitionWriter.write(schema, rowSinkRecord)
    topicPartitionWriter.processLineCount shouldBe 1
    topicPartitionWriter.write(schema, rowSinkRecord)
    topicPartitionWriter.processLineCount shouldBe 2
  }

  @Test
  def testDefaultValueCommitFile(): Unit = {
    val hdfsSinkConnectorConfig = HDFSSinkConnectorConfig(Map(HDFS_URL -> "", TMP_DIR -> "/tmp"))
    val sinkTaskContext = mock[RowSinkContext]

    val topicPartition = new TopicPartition("topic1", 1)
    val storage = mock[Storage]
    when(storage.open(anyString(), anyBoolean())).thenReturn(mock[OutputStream])
    when(storage.list("/data/partition1"))
      .thenReturn(List("prefix-000000001-000001000.csv", "prefix-000001001-000002000.csv").toIterator)

    val recordWriterOutput = mock[RecordWriterOutput]
    val topicPartitionWriter =
      new TopicPartitionWriter(hdfsSinkConnectorConfig, sinkTaskContext, topicPartition, storage)

    val rowSinkRecord = mock[RowSinkRecord]
    when(rowSinkRecord.row).thenReturn(Row.of(Cell.of("column1", "value")))

    val schema: Seq[Column] = Seq()
    topicPartitionWriter.recordWriterOutput = recordWriterOutput
    for (i <- 1 to 998) {
      topicPartitionWriter.write(schema, rowSinkRecord)
      topicPartitionWriter.processLineCount shouldBe i
    }
    topicPartitionWriter.write(schema, rowSinkRecord)
    topicPartitionWriter.processLineCount shouldBe 999
  }

  @Test
  def testIsDataCountCommit(): Unit = {
    val topicPartition = new TopicPartition("topic1", 0)

    val sinkTaskContext = mock[RowSinkContext]
    val hdfsSinkConnectorConfig = HDFSSinkConnectorConfig(Map(HDFS_URL -> "", TMP_DIR -> "/tmp"))
    val storage = mock[Storage]
    when(storage.list("/data/partition0"))
      .thenReturn(List("prefix-000000001-000001000.csv", "prefix-000001001-000002000.csv").toIterator)

    val topicPartitionWriter =
      new TopicPartitionWriter(hdfsSinkConnectorConfig, sinkTaskContext, topicPartition, storage)

    topicPartitionWriter.isDataCountCommit(0, 100) shouldBe false
    topicPartitionWriter.isDataCountCommit(10, 100) shouldBe false
    topicPartitionWriter.isDataCountCommit(100, 100) shouldBe true
    topicPartitionWriter.isDataCountCommit(101, 100) shouldBe true
  }

  @Test
  def testIsTimeCommit(): Unit = {
    val topicPartition = new TopicPartition("topic1", 0)

    val sinkTaskContext = mock[RowSinkContext]
    val hdfsSinkConnectorConfig = HDFSSinkConnectorConfig(Map(HDFS_URL -> "", TMP_DIR -> "/tmp"))
    val storage = mock[Storage]
    when(storage.list("/data/partition0"))
      .thenReturn(List("prefix-000000001-000001000.csv", "prefix-000001001-000002000.csv").toIterator)

    val topicPartitionWriter =
      new TopicPartitionWriter(hdfsSinkConnectorConfig, sinkTaskContext, topicPartition, storage)

    val startTime: Long = CommonUtil.current() - 1000
    topicPartitionWriter.isTimeCommit(startTime, 2000) shouldBe false //Not commit
    topicPartitionWriter.isTimeCommit(startTime, 1000) shouldBe true
  }

  @Test
  def testWriteByTime(): Unit = {
    val topicPartition = new TopicPartition("topic1", 0)

    val sinkTaskContext = mock[RowSinkContext]
    val hdfsSinkConnectorConfig = HDFSSinkConnectorConfig(
      Map(HDFS_URL -> "", TMP_DIR -> "/tmp", ROTATE_INTERVAL_MS -> "1500"))
    val storage = mock[Storage]
    when(storage.open(anyString(), anyBoolean())).thenReturn(mock[OutputStream])
    when(storage.list("/data/partition0"))
      .thenReturn(List("prefix-000000001-000001000.csv", "prefix-000001001-000002000.csv").toIterator)

    val recordWriterOutput = mock[RecordWriterOutput]
    val topicPartitionWriter =
      new TopicPartitionWriter(hdfsSinkConnectorConfig, sinkTaskContext, topicPartition, storage)

    val rowSinkRecord = mock[RowSinkRecord]
    when(rowSinkRecord.row).thenReturn(Row.of(Cell.of("column1", "value")))

    topicPartitionWriter.recordWriterOutput = recordWriterOutput
    topicPartitionWriter.processLineCount = 0

    val schema: Seq[Column] = Seq()
    topicPartitionWriter.write(schema, rowSinkRecord)
  }

  @Test
  def testFlushFilePath1(): Unit = {
    val topicPartition = new TopicPartition("topic1", 0)

    val sinkTaskContext = mock[RowSinkContext]
    val hdfsSinkConnectorConfig = HDFSSinkConnectorConfig(
      Map(HDFS_URL -> "", TMP_DIR -> "/tmp", ROTATE_INTERVAL_MS -> "1500"))
    val storage = mock[Storage]

    val topicPartitionWriter =
      new TopicPartitionWriter(hdfsSinkConnectorConfig, sinkTaskContext, topicPartition, storage)

    val dataDir = "/data/partition1"
    val result = topicPartitionWriter.flushFilePath(Iterator(), dataDir)
    result shouldBe "/data/partition1/part-000000000-000000000.csv"
  }

  @Test
  def testFlushFilePath2(): Unit = {
    val topicPartition = new TopicPartition("topic1", 0)
    val sinkTaskContext = mock[RowSinkContext]
    val hdfsSinkConnectorConfig = HDFSSinkConnectorConfig(Map(HDFS_URL -> ""))
    val storage = mock[Storage]

    val topicPartitionWriter =
      new TopicPartitionWriter(hdfsSinkConnectorConfig, sinkTaskContext, topicPartition, storage)

    topicPartitionWriter.processLineCount = 1000
    val dataDir = "/data/partition1"
    var flushFilePath = topicPartitionWriter.flushFilePath(Iterator(), dataDir)
    flushFilePath shouldBe "/data/partition1/part-000000000-000000999.csv"

    flushFilePath = topicPartitionWriter.flushFilePath(Iterator("part-000000000-000000999.csv"), dataDir)
    flushFilePath shouldBe "/data/partition1/part-000001000-000001999.csv"

    flushFilePath = topicPartitionWriter
      .flushFilePath(Iterator("part-000000000-000000999.csv", "part-000001000-000001999.csv"), dataDir)
    flushFilePath shouldBe "/data/partition1/part-000002000-000002999.csv"

    flushFilePath = topicPartitionWriter.flushFilePath(Iterator("part-000002000-000002999.csv"), dataDir)
    flushFilePath shouldBe "/data/partition1/part-000003000-000003999.csv"
  }

  @Test
  def testFlushFilePath3(): Unit = {
    val topicPartition = new TopicPartition("topic1", 0)
    val sinkTaskContext = mock[RowSinkContext]
    val hdfsSinkConnectorConfig = HDFSSinkConnectorConfig(Map(HDFS_URL -> ""))
    val storage = mock[Storage]

    val topicPartitionWriter =
      new TopicPartitionWriter(hdfsSinkConnectorConfig, sinkTaskContext, topicPartition, storage)

    val dataDir = "/data/partition1"

    topicPartitionWriter.processLineCount = 1000
    var flushFilePath = topicPartitionWriter.flushFilePath(Iterator(), dataDir)
    flushFilePath shouldBe "/data/partition1/part-000000000-000000999.csv"

    topicPartitionWriter.processLineCount = 500
    flushFilePath = topicPartitionWriter.flushFilePath(Iterator("part-000000000-000000999.csv"), dataDir)
    flushFilePath shouldBe "/data/partition1/part-000001000-000001499.csv"

    topicPartitionWriter.processLineCount = 222
    flushFilePath = topicPartitionWriter.flushFilePath(Iterator("part-000001000-000001499.csv"), dataDir)
    flushFilePath shouldBe "/data/partition1/part-000001500-000001721.csv"
  }
}
