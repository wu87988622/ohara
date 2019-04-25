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

package com.island.ohara.connector.perf
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.data._
import com.island.ohara.kafka.Consumer
import com.island.ohara.kafka.Consumer.Record
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
class TestPerfSource extends With3Brokers3Workers with Matchers {
  private[this] val workerClient = WorkerClient(testUtil.workersConnProps)

  private[this] val props = PerfSourceProps(
    batch = 5,
    freq = 5 seconds
  )

  private[this] val schema: Seq[Column] =
    Seq(
      Column.builder().name("a").dataType(DataType.STRING).order(1).build(),
      Column.builder().name("b").dataType(DataType.SHORT).order(2).build(),
      Column.builder().name("c").dataType(DataType.INT).order(3).build(),
      Column.builder().name("d").dataType(DataType.LONG).order(4).build(),
      Column.builder().name("e").dataType(DataType.FLOAT).order(5).build(),
      Column.builder().name("f").dataType(DataType.DOUBLE).order(6).build(),
      Column.builder().name("g").dataType(DataType.BOOLEAN).order(7).build(),
      Column.builder().name("h").dataType(DataType.BYTE).order(8).build(),
      Column.builder().name("i").dataType(DataType.BYTES).order(9).build()
    )

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  private[this] def pollData(topicName: String,
                             timeout: scala.concurrent.duration.Duration,
                             size: Int): Seq[Record[Row, Array[Byte]]] = {
    val consumer = Consumer
      .builder[Row, Array[Byte]]()
      .topicName(topicName)
      .offsetFromBegin()
      .connectionProps(testUtil.brokersConnProps)
      .keySerializer(Serializer.ROW)
      .valueSerializer(Serializer.BYTES)
      .build()
    try consumer.poll(java.time.Duration.ofNanos(timeout.toNanos), size).asScala
    finally consumer.close()
  }

  private[this] def matchType(lhs: Class[_], dataType: DataType): Unit = {
    dataType match {
      case DataType.STRING  => lhs shouldBe classOf[String]
      case DataType.SHORT   => lhs shouldBe classOf[java.lang.Short]
      case DataType.INT     => lhs shouldBe classOf[java.lang.Integer]
      case DataType.LONG    => lhs shouldBe classOf[java.lang.Long]
      case DataType.FLOAT   => lhs shouldBe classOf[java.lang.Float]
      case DataType.DOUBLE  => lhs shouldBe classOf[java.lang.Double]
      case DataType.BOOLEAN => lhs shouldBe classOf[java.lang.Boolean]
      case DataType.BYTE    => lhs shouldBe classOf[java.lang.Byte]
      case DataType.BYTES   => lhs shouldBe classOf[Array[Byte]]
      case _                => throw new IllegalArgumentException("unsupported type in testing TestPerfSource")
    }
  }

  @Test
  def testNormalCase(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[PerfSource])
        .numberOfTasks(1)
        .id(connectorName)
        .columns(schema)
        .settings(props.toMap)
        .create)

    try {
      PerfUtils.checkConnector(testUtil, connectorName)
      // it is hard to evaluate number from records in topics so we just fetch some records here.
      val records = pollData(topicName, props.freq * 3, props.batch)
      records.size >= props.batch shouldBe true
      records
        .map(_.key.get)
        .foreach(row => {
          row.size shouldBe schema.size
          schema.foreach(c => {
            val cell: Cell[_] = row.cell(c.order - 1)
            cell.name shouldBe c.name
            matchType(cell.value.getClass, c.dataType)
          })
        })
    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testNormalCaseWithoutBatch(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[PerfSource])
        .numberOfTasks(1)
        .id(connectorName)
        .columns(schema)
        .settings(Map(PERF_FREQUENCE -> "5 seconds"))
        .create)

    try {
      PerfUtils.checkConnector(testUtil, connectorName)
      // it is hard to evaluate number from records in topics so we just fetch some records here.
      val records = pollData(topicName, props.freq * 3, props.batch)
      records.size >= props.batch shouldBe true
      records
        .map(_.key.get)
        .foreach(row => {
          row.size shouldBe schema.size
          schema.foreach(c => {
            val cell: Cell[_] = row.cell(c.order - 1)
            cell.name shouldBe c.name
            matchType(cell.value.getClass, c.dataType)
          })
        })
    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testNormalCaseWithoutFrequence(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[PerfSource])
        .numberOfTasks(1)
        .id(connectorName)
        .columns(schema)
        .settings(Map(PERF_BATCH -> "5"))
        .create)

    try {
      PerfUtils.checkConnector(testUtil, connectorName)
      // it is hard to evaluate number from records in topics so we just fetch some records here.
      val records = pollData(topicName, props.freq * 3, props.batch)
      records.size >= props.batch shouldBe true
      records
        .map(_.key.get)
        .foreach(row => {
          row.size shouldBe schema.size
          schema.foreach(c => {
            val cell: Cell[_] = row.cell(c.order - 1)
            cell.name shouldBe c.name
            matchType(cell.value.getClass, c.dataType)
          })
        })
    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testNormalCaseWithoutInput(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[PerfSource])
        .numberOfTasks(1)
        .id(connectorName)
        .columns(schema)
        .settings(Map.empty)
        .create)

    try {
      PerfUtils.checkConnector(testUtil, connectorName)
      // it is hard to evaluate number from records in topics so we just fetch some records here.
      val records = pollData(topicName, props.freq * 3, props.batch)
      records.size >= props.batch shouldBe true
      records
        .map(_.key.get)
        .foreach(row => {
          row.size shouldBe schema.size
          schema.foreach(c => {
            val cell: Cell[_] = row.cell(c.order - 1)
            cell.name shouldBe c.name
            matchType(cell.value.getClass, c.dataType)
          })
        })
    } finally result(workerClient.delete(connectorName))
  }

  @Test
  def testInvalidInput(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[PerfSource])
        .numberOfTasks(1)
        .id(connectorName)
        .columns(schema)
        .settings(Map(PERF_FREQUENCE -> "abcd"))
        .create)
    PerfUtils.assertFailedConnector(testUtil, connectorName)
  }

  @Test
  def testInvalidInputWithNegative(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[PerfSource])
        .numberOfTasks(1)
        .id(connectorName)
        .columns(schema)
        .settings(Map(PERF_BATCH -> "-1"))
        .create)
    PerfUtils.assertFailedConnector(testUtil, connectorName)
  }
}
