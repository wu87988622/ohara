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

import com.island.ohara.client.configurator.v0.ValidationApi
import com.island.ohara.client.configurator.v0.ValidationApi.{
  FtpValidation,
  HdfsValidation,
  RdbValidation,
  RdbValidationReport,
  ValidationReport
}
import com.island.ohara.client.kafka.{TopicAdmin, WorkerClient}
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.setting.ConnectorKey
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.kafka.Consumer
import spray.json.{JsNull, JsObject, JsValue}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
object ValidationUtils {
  private[this] val TIMEOUT = 30 seconds

  def run(workerClient: WorkerClient, topicAdmin: TopicAdmin, request: RdbValidation, taskCount: Int)(
    implicit executionContext: ExecutionContext): Future[Seq[RdbValidationReport]] = run(
    workerClient,
    topicAdmin,
    ValidationApi.VALIDATION_RDB_PREFIX_PATH,
    ValidationApi.RDB_VALIDATION_JSON_FORMAT.write(request).asJsObject.fields,
    taskCount
  ).map {
    _.filter(_.isInstanceOf[RdbValidationReport]).map(_.asInstanceOf[RdbValidationReport])
  }

  def run(workerClient: WorkerClient, topicAdmin: TopicAdmin, request: HdfsValidation, taskCount: Int)(
    implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] = run(
    workerClient,
    topicAdmin,
    ValidationApi.VALIDATION_HDFS_PREFIX_PATH,
    ValidationApi.HDFS_VALIDATION_JSON_FORMAT.write(request).asJsObject.fields,
    taskCount
  ).map {
    _.filter(_.isInstanceOf[ValidationReport]).map(_.asInstanceOf[ValidationReport])
  }

  def run(workerClient: WorkerClient, topicAdmin: TopicAdmin, request: FtpValidation, taskCount: Int)(
    implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] = run(
    workerClient,
    topicAdmin,
    ValidationApi.VALIDATION_FTP_PREFIX_PATH,
    ValidationApi.FTP_VALIDATION_JSON_FORMAT.write(request).asJsObject.fields,
    taskCount
  ).map {
    _.filter(_.isInstanceOf[ValidationReport]).map(_.asInstanceOf[ValidationReport])
  }

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
                        settings: Map[String, JsValue],
                        taskCount: Int)(implicit executionContext: ExecutionContext): Future[Seq[Any]] = {
    val requestId: String = CommonUtils.randomString()
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), s"Validator-${CommonUtils.randomString()}")
    workerClient
      .connectorCreator()
      .connectorKey(connectorKey)
      .className("com.island.ohara.connector.validation.Validator")
      .numberOfTasks(taskCount)
      .topicKey(ValidationApi.INTERNAL_TOPIC_KEY)
      .settings(Map(
        ValidationApi.SETTINGS_KEY -> JsObject(settings.filter {
          case (_, value) =>
            value match {
              case JsNull => false
              case _      => true
            }
        }).toString(),
        ValidationApi.REQUEST_ID -> requestId,
        ValidationApi.TARGET_KEY -> target
      ))
      .threadPool(executionContext)
      .create()
      .map { _ =>
        // TODO: receiving all messages may be expensive...by chia
        val client = Consumer
          .builder()
          .connectionProps(topicAdmin.connectionProps)
          .offsetFromBegin()
          .topicName(ValidationApi.INTERNAL_TOPIC_KEY.topicNameOnKafka)
          .keySerializer(Serializer.STRING)
          .valueSerializer(Serializer.OBJECT)
          .build()

        val buffer: mutable.Buffer[Any] = mutable.Buffer[Any]()
        try while (buffer.size < taskCount) {
          client
            .poll(java.time.Duration.ofMillis(TIMEOUT.toMillis))
            .asScala
            .filter(_.key().isPresent)
            .filter(_.key().get.equals(requestId))
            .filter(_.value().isPresent)
            .map(_.value().get())
            .foreach(buffer += _)
        } finally Releasable.close(client)
        buffer
      }
      .flatMap(r => workerClient.delete(connectorKey).map(_ => r))
  }
}
