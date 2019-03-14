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

import com.island.ohara.common.data.Column
import com.island.ohara.kafka.connector.{RowSinkContext, TopicPartition}
import com.island.ohara.testing.WithTestUtils
import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar

class TestDataWriter extends WithTestUtils with Matchers with MockitoSugar {

  @Test
  def testCreatePartitionDataWriters(): Unit = {
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
