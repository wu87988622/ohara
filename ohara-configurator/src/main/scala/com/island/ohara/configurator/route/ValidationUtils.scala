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

import java.util

import com.island.ohara.client.configurator.v0.ValidationApi
import com.island.ohara.client.configurator.v0.ValidationApi.{
  FtpValidationRequest,
  HdfsValidationRequest,
  JdbcValidationReport,
  RdbValidationRequest,
  ValidationReport
}
import com.island.ohara.client.kafka.{TopicAdmin, WorkerClient}
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.Consumer
import com.island.ohara.kafka.Consumer.Record
import spray.json.{JsNull, JsNumber, JsString}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object ValidationUtils {
  private[this] val TIMEOUT = 30 seconds

  def run(workerClient: WorkerClient, topicAdmin: TopicAdmin, request: RdbValidationRequest, taskCount: Int)(
    implicit executionContext: ExecutionContext): Future[Seq[JdbcValidationReport]] = run(
    workerClient,
    topicAdmin,
    ValidationApi.VALIDATION_RDB_PREFIX_PATH,
    ValidationApi.RDB_VALIDATION_REQUEST_JSON_FORMAT.write(request).asJsObject.fields.map {
      case (k, v) => (k, v.asInstanceOf[JsString].value)
    },
    taskCount
  ).map(_.map {
    case report: JdbcValidationReport => report
    case report: Any =>
      throw new IllegalArgumentException(s"what is this??? ${report.getClass.getName}")
  })

  def run(workerClient: WorkerClient, topicAdmin: TopicAdmin, request: HdfsValidationRequest, taskCount: Int)(
    implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] = run(
    workerClient,
    topicAdmin,
    ValidationApi.VALIDATION_HDFS_PREFIX_PATH,
    ValidationApi.HDFS_VALIDATION_REQUEST_JSON_FORMAT.write(request).asJsObject.fields.map {
      case (k, v) => (k, v.asInstanceOf[JsString].value)
    },
    taskCount
  )

  def run(workerClient: WorkerClient, topicAdmin: TopicAdmin, request: FtpValidationRequest, taskCount: Int)(
    implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] = run(
    workerClient,
    topicAdmin,
    ValidationApi.VALIDATION_FTP_PREFIX_PATH,
    ValidationApi.FTP_VALIDATION_REQUEST_JSON_FORMAT
      .write(request)
      .asJsObject
      .fields
      .map {
        case (k, v) =>
          v match {
            case s: JsString => Some((k, s.value))
            // port is Int type
            case n: JsNumber => Some((k, n.value.toString))
            // worker cluster name is useless here
            case JsNull => None
            case _      => throw new IllegalArgumentException("what is this??")
          }
      }
      .flatten
      .toMap,
    taskCount
  )

  /**
    * a helper method to run the validation process quickly.
    *
    * @param workerClient connector client
    * @param topicAdmin topic admin
    * @param settings setting used to test
    * @param taskCount the number from task. It implies how many worker nodes should be verified
    * @return reports
    */
  private[this] def run(workerClient: WorkerClient,
                        topicAdmin: TopicAdmin,
                        target: String,
                        settings: Map[String, String],
                        taskCount: Int)(implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] = {
    val requestId: String = CommonUtils.uuid()
    val validationName = s"Validator-${CommonUtils.randomString()}"
    workerClient
      .connectorCreator()
      .id(validationName)
      .className("com.island.ohara.connector.validation.Validator")
      .numberOfTasks(taskCount)
      .topicName(ValidationApi.INTERNAL_TOPIC)
      .settings(
        settings ++ Map(
          ValidationApi.REQUEST_ID -> requestId,
          ValidationApi.TARGET -> target
        ))
      .create
      .map { _ =>
        // TODO: receiving all messages may be expensive...by chia
        val client = Consumer
          .builder[String, Object]()
          .connectionProps(topicAdmin.connectionProps)
          .offsetFromBegin()
          .topicName(ValidationApi.INTERNAL_TOPIC)
          .keySerializer(Serializer.STRING)
          .valueSerializer(Serializer.OBJECT)
          .build()
        try client
          .poll(
            java.time.Duration.ofNanos(TIMEOUT.toNanos),
            taskCount,
            new java.util.function.Function[util.List[Record[String, Object]], util.List[Record[String, Object]]] {
              override def apply(records: util.List[Record[String, Object]]): util.List[Record[String, Object]] = {
                records.asScala.filter(requestId == _.key.orElse(null)).asJava
              }
            }
          )
          .asScala
          .map(_.value.get match {
            case report: ValidationReport => report
            case _                        => throw new IllegalStateException(s"Unknown report")
          })
        finally workerClient.delete(validationName)
      }
  }
}
