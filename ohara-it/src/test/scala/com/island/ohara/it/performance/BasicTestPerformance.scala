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

package com.island.ohara.it.performance

import java.io.{File, FileWriter}
import java.util.concurrent.atomic.{AtomicBoolean, LongAdder}
import java.util.concurrent.{Executors, TimeUnit}

import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorInfo
import com.island.ohara.client.configurator.v0.MetricsApi.Meter
import com.island.ohara.client.configurator.v0.TopicApi.TopicInfo
import com.island.ohara.client.configurator.v0.{ConnectorApi, TopicApi}
import com.island.ohara.common.data.{Cell, Row, Serializer}
import com.island.ohara.common.setting.{ConnectorKey, TopicKey}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.it.WithRemoteWorkers
import com.island.ohara.kafka.Producer
import com.typesafe.scalalogging.Logger
import org.junit.rules.Timeout
import org.junit.{After, Rule}
import spray.json.JsValue

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * the basic infra to test performance for ohara components.
  * all pre-defined configs in this class should NOT be changed arbitrary since they are related to our jenkins.
  *
  * Noted:
  * 1) the sub implementation should have single test case in order to avoid complicated code and timeout
  * 2) the sub implementation does NOT need to generate any report or output since this infra traces all metrics of connector
  *    and topics for sub implementation
  * 3) the reports are located at /tmp/performance/$className/$testName/$random.csv by default. Of course, this is related to jenkins
  *    so please don't change it.
  */
abstract class BasicTestPerformance extends WithRemoteWorkers {
  protected val log: Logger      = Logger(classOf[BasicTestPerformance])
  private[this] val wholeTimeout = 1200

  @Rule
  override def timeout: Timeout = Timeout.seconds(wholeTimeout) // 20 minutes

  protected val topicApi: TopicApi.Access =
    TopicApi.access
      .hostname(configuratorHostname)
      .port(configuratorPort)

  protected val connectorApi: ConnectorApi.Access =
    ConnectorApi.access
      .hostname(configuratorHostname)
      .port(configuratorPort)

  //------------------------------[global properties]------------------------------//
  private[this] val durationOfTestPerformanceKey     = "durationOfTestPerformance"
  private[this] val durationOfTestPerformanceDefault = 30 seconds
  private[this] val durationOfTestPerformance = {
    val v = value(durationOfTestPerformanceKey).map(Duration.apply).getOrElse(durationOfTestPerformanceDefault)
    // too big duration is never completed
    if (v.toSeconds > wholeTimeout / 2) throw new AssertionError(s"the max duration is ${wholeTimeout / 2} seconds")
    v
  }

  private[this] val csvOutputFolderKey = "csvOutputFolder"
  private[this] val csvOutputFolder: File = mkdir(
    new File(
      value(csvOutputFolderKey)
        .getOrElse("/tmp/performance")
    )
  )

  //------------------------------[topic properties]------------------------------//
  private[this] val numberOfProducerThreadKey     = "numberOfProducerThread"
  private[this] val numberOfProducerThreadDefault = 2
  private[this] val numberOfProducerThread =
    value(numberOfProducerThreadKey).map(_.toInt).getOrElse(numberOfProducerThreadDefault)

  private[this] val megabytesOfInputDataKey           = "megabytesOfInputData"
  private[this] val megabytesOfInputDataDefault: Long = 100
  private[this] val sizeOfInputData =
    1024L * 1024L * value(megabytesOfInputDataKey).map(_.toLong).getOrElse(megabytesOfInputDataDefault)

  private[this] val durationOfTestProducingInputDataKey = "durationOfTestProducingInputData"
  private[this] val durationOfTestProducingInputDataDefault =
    (durationOfTestPerformanceDefault.toMillis * 10) milliseconds
  private[this] val durationOfTestProducingInputData =
    value(durationOfTestProducingInputDataKey).map(Duration.apply).getOrElse(durationOfTestProducingInputDataDefault)
  //------------------------------[producer properties]------------------------------//
  private[this] val numberOfPartitionsKey     = "numberOfPartitions"
  private[this] val numberOfPartitionsDefault = 1
  protected val numberOfPartitions: Int =
    value(numberOfPartitionsKey).map(_.toInt).getOrElse(numberOfPartitionsDefault)

  private[this] val numberOfRowsToFlushKey     = "numberOfRowsToFlush"
  private[this] val numberOfRowsToFlushDefault = 10
  private[this] val numberOfRowsToFlush =
    value(numberOfRowsToFlushKey).map(_.toInt).getOrElse(numberOfRowsToFlushDefault)

  private[this] val cellNamesKey           = "cellNames"
  private[this] val cellNamesDefault       = Set("c0", "c1", "c2")
  private[this] val cellNames: Set[String] = value(cellNamesKey).map(_.split(",").toSet).getOrElse(cellNamesDefault)

  //------------------------------[connector properties]------------------------------//
  private[this] val numberOfConnectorTasksKey     = "numberOfConnectorTasks"
  private[this] val numberOfConnectorTasksDefault = 1
  protected val numberOfConnectorTasks: Int =
    value(numberOfConnectorTasksKey).map(_.toInt).getOrElse(numberOfConnectorTasksDefault)

  private[this] def value(key: String): Option[String] = sys.env.get(key)
  //------------------------------[helper methods]------------------------------//
  private[this] def mkdir(folder: File): File = {
    if (!folder.exists() && !folder.mkdirs()) throw new AssertionError(s"failed to create folder on $folder")
    if (folder.exists() && !folder.isDirectory) throw new AssertionError(s"$folder is not a folder")
    folder
  }

  protected def sleepUntilEnd(): Long = {
    TimeUnit.MILLISECONDS.sleep(durationOfTestPerformance.toMillis)
    durationOfTestPerformance.toMillis
  }

  /**
    * create the topic and setup the data.
    * @param topicKey topic key
    * @return (topic info, number of rows, size (in bytes) of all input rows)
    */
  protected def setupTopic(topicKey: TopicKey): (TopicInfo, Long, Long) = {
    result(
      topicApi.request
        .key(topicKey)
        .brokerClusterKey(brokerClusterInfo.key)
        .numberOfPartitions(numberOfPartitions)
        .create()
    )
    await(() => {
      result(topicApi.start(topicKey))
      true
    }, true)
    produce(result(topicApi.get(topicKey)))
  }

  protected def setupConnector(
    connectorKey: ConnectorKey,
    topicKey: TopicKey,
    className: String,
    settings: Map[String, JsValue]
  ): ConnectorInfo = {
    result(
      connectorApi.request
        .settings(settings)
        .key(connectorKey)
        .className(className)
        .topicKey(topicKey)
        .workerClusterKey(workerClusterInfo.key)
        .numberOfTasks(numberOfConnectorTasks)
        .create()
    )
    await(
      () => {
        result(connectorApi.start(connectorKey))
        true
      },
      true
    )
    result(connectorApi.get(connectorKey))
  }

  private[this] def produce(topicInfo: TopicInfo): (TopicInfo, Long, Long) = {
    val pool        = Executors.newFixedThreadPool(numberOfProducerThread)
    val closed      = new AtomicBoolean(false)
    val count       = new LongAdder()
    val sizeInBytes = new LongAdder()
    try {
      (0 until numberOfProducerThread).foreach { _ =>
        pool.execute(() => {
          val producer = Producer
            .builder()
            .keySerializer(Serializer.ROW)
            .connectionProps(brokerClusterInfo.connectionProps)
            .build()
          var cachedRows = 0
          try while (!closed.get() && sizeInBytes.longValue() <= sizeOfInputData) {
            producer
              .sender()
              .topicName(topicInfo.key.topicNameOnKafka())
              .key(Row.of(cellNames.map { name =>
                Cell.of(name, CommonUtils.randomString())
              }.toSeq: _*))
              .send()
              .whenComplete {
                case (meta, _) =>
                  if (meta != null) {
                    sizeInBytes.add(meta.serializedKeySize())
                    count.add(1)
                  }
              }
            cachedRows += 1
            if (cachedRows >= numberOfRowsToFlush) {
              producer.flush()
              cachedRows = 0
            }
          } finally Releasable.close(producer)
        })
      }
    } finally {
      pool.shutdown()
      pool.awaitTermination(durationOfTestProducingInputData.toMillis, TimeUnit.MILLISECONDS)
      closed.set(true)
    }
    (topicInfo, count.longValue(), sizeInBytes.longValue())
  }

  //------------------------------[core functions]------------------------------//

  @After
  def record(): Unit = {
    def simpleName(className: String): String = {
      val index = className.lastIndexOf(".")
      if (index != -1) className.substring(index + 1)
      else className
    }

    // $OUTPUT/className/testName/$RANDOM.csv
    def path(className: String): File =
      new File(
        mkdir(new File(mkdir(new File(csvOutputFolder, simpleName(className))), this.getClass.getSimpleName)),
        s"${CommonUtils.randomString(10)}.csv"
      )

    // record connector meters
    val connectorInfos = result(connectorApi.list())
    connectorInfos
      .map(_.className)
      .toSet[String]
      .foreach(
        className =>
          recordCsv(path(className), connectorInfos.filter(_.className == className).flatMap(_.metrics.meters))
      )

    // record topic meters
    val topicMeters = result(topicApi.list()).flatMap(_.metrics.meters)
    if (topicMeters.nonEmpty) recordCsv(path("topic"), topicMeters)
  }

  private[this] def recordCsv(file: File, meters: Seq[Meter]): Unit =
    recordCsv(file, meters.map { m =>
      m.name -> meters.filter(_.name == m.name).map(_.value).sum
    }.toMap)

  private[this] def recordCsv(file: File, items: Map[String, Double]): Unit = {
    // we have to fix the order of key-value
    // if we generate line via map.keys and map.values, the order may be different ...
    val headers    = items.keys.toList
    val values     = headers.map(items(_)).mkString(",")
    val fileWriter = new FileWriter(file)
    try {
      fileWriter.write(headers.map(s => s"""\"$s\"""").mkString(","))
      fileWriter.write("\n")
      fileWriter.write(values)
    } finally Releasable.close(fileWriter)
  }
}
