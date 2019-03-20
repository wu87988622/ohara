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
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.Await
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
      Column.newBuilder().name("a").dataType(DataType.STRING).order(1).build(),
      Column.newBuilder().name("b").dataType(DataType.SHORT).order(2).build(),
      Column.newBuilder().name("c").dataType(DataType.INT).order(3).build(),
      Column.newBuilder().name("d").dataType(DataType.LONG).order(4).build(),
      Column.newBuilder().name("e").dataType(DataType.FLOAT).order(5).build(),
      Column.newBuilder().name("f").dataType(DataType.DOUBLE).order(6).build(),
      Column.newBuilder().name("g").dataType(DataType.BOOLEAN).order(7).build(),
      Column.newBuilder().name("h").dataType(DataType.BYTE).order(8).build(),
      Column.newBuilder().name("i").dataType(DataType.BYTES).order(9).build()
    )

  @Test
  def testNormalCase(): Unit = {
    val topicName = methodName
    val connectorName = methodName
    Await.result(
      workerClient
        .connectorCreator()
        .topicName(topicName)
        .connectorClass(classOf[PerfSource])
        .numberOfTasks(1)
        .name(connectorName)
        .columns(schema)
        .settings(props.toMap)
        .create,
      10 seconds
    )

    try {
      PerfUtils.checkConnector(testUtil, connectorName)
      val consumer =
        Consumer
          .builder[Row, Array[Byte]]()
          .connectionProps(testUtil.brokersConnProps)
          .offsetFromBegin()
          .topicName(topicName)
          .keySerializer(Serializer.ROW)
          .valueSerializer(Serializer.BYTES)
          .build()
      try {
        def matchType(lhs: Class[_], dataType: DataType): Unit = {
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
        // it is hard to evaluate number from records in topics so we just fetch some records here.

        val records = consumer.poll(java.time.Duration.ofNanos((props.freq * 3).toNanos), props.batch)
        records.size >= props.batch shouldBe true
        records.asScala
          .map(_.key.get)
          .foreach(row => {
            row.size shouldBe schema.size
            schema.foreach(c => {
              val cell: Cell[_] = row.cell(c.order - 1)
              cell.name shouldBe c.name
              matchType(cell.value.getClass, c.dataType)
            })

          })
      } finally consumer.close()
    } finally workerClient.delete(connectorName)
  }
}
