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

package com.island.ohara.configurator.route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{entity, _}
import com.island.ohara.agent.{BrokerCollie, WorkerCollie}
import com.island.ohara.client.configurator.v0.FileInfoApi.FileInfo
import com.island.ohara.client.configurator.v0.QueryApi._
import com.island.ohara.client.configurator.v0.ValidationApi.RdbValidation
import com.island.ohara.client.database.DatabaseClient
import com.island.ohara.common.data.{Row, Serializer}
import com.island.ohara.common.setting.{ObjectKey, TopicKey}
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.ReflectionUtils
import com.island.ohara.configurator.fake.FakeWorkerClient
import com.island.ohara.configurator.route.ObjectChecker.Condition.RUNNING
import com.island.ohara.configurator.store.DataStore
import com.island.ohara.kafka.Consumer.Record
import com.island.ohara.kafka.{Consumer, Header}
import spray.json.{DeserializationException, JsObject}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

/**
  * used to handle the "QUERY" APIs
  */
private[configurator] object QueryRoute {

  /**
    * we reuse the great conversion of JIO connectors
    * @param row row
    * @return json representation
    */
  def toJson(row: Row): JsObject = com.island.ohara.client.configurator.v0.toJson(row)

  private[this] def topicData(records: Seq[Record[Array[Byte], Array[Byte]]]): TopicData =
    TopicData(records
      .filter(_.key().isPresent)
      .map(record => (record.partition(), record.offset(), record.key().get(), record.headers().asScala))
      .map {
        case (partition, offset, bytes, headers) =>
          try Message(
            partition = partition,
            offset = offset,
            // only Ohara source connectors have this header
            sourceClass = headers.find(_.key() == Header.SOURCE_CLASS_KEY).map(h => new String(h.value())),
            sourceKey =
              headers.find(_.key() == Header.SOURCE_KEY_KEY).map(h => new String(h.value())).map(ObjectKey.toObjectKey),
            value = Some(toJson(Serializer.ROW.from(bytes))),
            error = None
          )
          catch {
            case e: Throwable =>
              Message(partition = partition,
                      offset = offset,
                      sourceClass = None,
                      sourceKey = None,
                      value = None,
                      error = Some(e.getMessage))
          }
      })

  def apply(implicit brokerCollie: BrokerCollie,
            adminCleaner: AdminCleaner,
            dataStore: DataStore,
            workerCollie: WorkerCollie,
            objectChecker: ObjectChecker,
            executionContext: ExecutionContext): server.Route = pathPrefix(QUERY_PREFIX_PATH) {
    path(RDB_PREFIX_PATH) {
      post {
        entity(as[RdbQuery]) { query =>
          complete(both(query.workerClusterKey).flatMap {
            case (_, topicAdmin, workerCluster, workerClient) =>
              workerClient match {
                case _: FakeWorkerClient =>
                  val client = DatabaseClient.builder.url(query.url).user(query.user).password(query.password).build
                  try Future.successful(RdbInfo(
                    client.databaseType,
                    client.tableQuery
                      .catalog(query.catalogPattern.orNull)
                      .schema(query.schemaPattern.orNull)
                      .tableName(query.tableName.orNull)
                      .execute()
                  ))
                  finally client.close()
                case _ =>
                  ValidationUtils
                    .run(
                      workerClient,
                      topicAdmin,
                      RdbValidation(
                        url = query.url,
                        user = query.user,
                        password = query.password,
                        workerClusterKey = workerCluster.key
                      ),
                      1
                    )
                    .map { reports =>
                      if (reports.isEmpty) throw new IllegalArgumentException("no report!!!")
                      reports
                        .find(_.rdbInfo.isDefined)
                        .map(_.rdbInfo.get)
                        .getOrElse(throw new IllegalStateException("failed to query table via ohara.Validator"))
                    }
              }
          })
        }
      }
    } ~ path(TOPIC_PREFIX_PATH / Segment) { topicName =>
      post {
        parameters(
          (GROUP_KEY ? GROUP_DEFAULT,
           TOPIC_TIMEOUT_KEY.as[Long] ? TOPIC_TIMEOUT_DEFAULT.toMillis,
           TOPIC_LIMIT_KEY.as[Int] ? TOPIC_LIMIT_DEFAULT)) { (group, timeoutMs, limit) =>
          val topicKey = TopicKey.of(group, topicName)
          if (limit <= 0)
            throw DeserializationException(s"the limit must be bigger than zero. actual:$limit",
                                           fieldNames = List(TOPIC_LIMIT_KEY))
          complete(
            objectChecker.checkList
              .topic(topicKey, RUNNING)
              .check()
              .map(_.topicInfos.head._1.brokerClusterKey)
              .flatMap(brokerClusterKey =>
                objectChecker.checkList
                  .brokerCluster(brokerClusterKey, RUNNING)
                  .check()
                  .map(_.runningBrokers.head.connectionProps))
              .map { connectionProps =>
                val consumer = Consumer
                  .builder()
                  .connectionProps(connectionProps)
                  .topicName(topicKey.topicNameOnKafka())
                  // move the offset to first place so we can break the wait if there is no data
                  .offsetFromBegin()
                  .build()
                try {
                  val endTime = CommonUtils.current() + timeoutMs
                  // first poll: it not only fetch data but also subscribe the partitions.
                  consumer.poll(java.time.Duration.ofMillis(timeoutMs), 1)
                  consumer.endOffsets().asScala.foreach {
                    case (tp, offset) => consumer.seek(tp, offset - limit)
                  }
                  topicData(
                    consumer
                    // even if the timeout reach the limit, we still give a last try :)
                      .poll(java.time.Duration.ofMillis(Math.max(1000L, endTime - CommonUtils.current())), limit)
                      .asScala)
                } finally Releasable.close(consumer)
              })
        }
      }
    } ~ path(FILE_PREFIX_PATH / Segment) { fileName =>
      parameter(GROUP_KEY ?) { groupOption =>
        complete(dataStore.value[FileInfo](ObjectKey.of(groupOption.getOrElse(GROUP_DEFAULT), fileName)).map {
          fileInfo =>
            val (sources, sinks, streamApps) = ReflectionUtils.loadConnectorAndStreamClasses(fileInfo)
            FileContent(
              sources.map(n => ClassInfo(SOURCE_CONNECTOR_KEY, n)) ++
                sinks.map(n => ClassInfo(SINK_CONNECTOR_KEY, n)) ++
                streamApps.map(n => ClassInfo(STREAM_APP_KEY, n))
            )
        })
      }
    }
  }
}
