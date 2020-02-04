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

package com.island.ohara.connector.ftp

import com.island.ohara.client.kafka.ConnectorAdmin
import com.island.ohara.common.setting.SettingDef.Necessary
import com.island.ohara.common.setting.{ConnectorKey, TopicKey}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.ConnectorDefUtils
import com.island.ohara.testing.WithBrokerWorker
import org.junit.Test
import org.scalatest.Matchers._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestFtpDefinition extends WithBrokerWorker {
  private[this] val connectorAdmin = ConnectorAdmin(testUtil().workersConnProps())

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def testSource(): Unit = {
    val topicKey     = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val response = result(
      connectorAdmin
        .connectorValidator()
        .connectorKey(connectorKey)
        .numberOfTasks(1)
        .topicKey(topicKey)
        .connectorClass(classOf[FtpSource])
        .run()
    )

    response.settings().size should not be 0
    response
      .settings()
      .asScala
      .filter(_.definition().key() == ConnectorDefUtils.TOPIC_NAMES_DEFINITION.key())
      .head
      .definition()
      .necessary() shouldBe Necessary.REQUIRED
    response
      .settings()
      .asScala
      .filter(_.definition().key() == ConnectorDefUtils.CONNECTOR_CLASS_DEFINITION.key())
      .head
      .definition()
      .necessary() shouldBe Necessary.REQUIRED
    response
      .settings()
      .asScala
      .filter(_.definition().key() == ConnectorDefUtils.NUMBER_OF_TASKS_DEFINITION.key())
      .head
      .definition()
      .necessary() shouldBe Necessary.OPTIONAL
    response
      .settings()
      .asScala
      .filter(_.definition().key() == ConnectorDefUtils.COLUMNS_DEFINITION.key())
      .head
      .definition()
      .necessary() should not be Necessary.REQUIRED
    response
      .settings()
      .asScala
      .filter(_.definition().key() == ConnectorDefUtils.WORKER_CLUSTER_KEY_DEFINITION.key())
      .head
      .definition()
      .necessary() shouldBe Necessary.REQUIRED
    // we don't pass all arguments so it should contain error.
    response.errorCount() should not be 0
  }

  @Test
  def testSink(): Unit = {
    val topicKey     = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val response = result(
      connectorAdmin
        .connectorValidator()
        .connectorKey(connectorKey)
        .numberOfTasks(1)
        .topicKey(topicKey)
        .connectorClass(classOf[FtpSink])
        .run()
    )

    response.settings().size should not be 0
    response
      .settings()
      .asScala
      .filter(_.definition().key() == ConnectorDefUtils.TOPIC_NAMES_DEFINITION.key())
      .head
      .definition()
      .necessary() shouldBe Necessary.REQUIRED
    response
      .settings()
      .asScala
      .filter(_.definition().key() == ConnectorDefUtils.CONNECTOR_CLASS_DEFINITION.key())
      .head
      .definition()
      .necessary() shouldBe Necessary.REQUIRED
    response
      .settings()
      .asScala
      .filter(_.definition().key() == ConnectorDefUtils.NUMBER_OF_TASKS_DEFINITION.key())
      .head
      .definition()
      .necessary() shouldBe Necessary.OPTIONAL
    response
      .settings()
      .asScala
      .filter(_.definition().key() == ConnectorDefUtils.COLUMNS_DEFINITION.key())
      .head
      .definition()
      .necessary() should not be Necessary.REQUIRED
    response
      .settings()
      .asScala
      .filter(_.definition().key() == ConnectorDefUtils.WORKER_CLUSTER_KEY_DEFINITION.key())
      .head
      .definition()
      .necessary() shouldBe Necessary.REQUIRED
    response.errorCount() should not be 0
  }
}
