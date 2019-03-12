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

package com.island.ohara.client.kafka

import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestConnectorCreator extends SmallTest with Matchers {

  private[this] val notWorkingClient = WorkerClient("localhost:2222")

  @Test
  def nullConfigs(): Unit =
    an[NullPointerException] should be thrownBy notWorkingClient.connectorCreator().configs(null)

  @Test
  def nullSchema(): Unit = an[NullPointerException] should be thrownBy notWorkingClient.connectorCreator().schema(null)

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy notWorkingClient.connectorCreator().name(null)

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy notWorkingClient.connectorCreator().name("")

  @Test
  def illegalNumberOfTasks(): Unit =
    an[IllegalArgumentException] should be thrownBy notWorkingClient.connectorCreator().numberOfTasks(-1)

  @Test
  def nullClass(): Unit = an[NullPointerException] should be thrownBy notWorkingClient
    .connectorCreator()
    .connectorClass(null.asInstanceOf[Class[_]])

  @Test
  def nullClassName(): Unit = an[NullPointerException] should be thrownBy notWorkingClient
    .connectorCreator()
    .connectorClass(null.asInstanceOf[String])

  @Test
  def emptyClassName(): Unit =
    an[IllegalArgumentException] should be thrownBy notWorkingClient.connectorCreator().connectorClass("")

  @Test
  def nullTopicName(): Unit =
    an[NullPointerException] should be thrownBy notWorkingClient.connectorCreator().topicName(null)

  @Test
  def emptyTopicName(): Unit =
    an[IllegalArgumentException] should be thrownBy notWorkingClient.connectorCreator().topicName("")

  @Test
  def nullTopicNames(): Unit =
    an[NullPointerException] should be thrownBy notWorkingClient.connectorCreator().topicNames(null)

  @Test
  def emptyTopicNames(): Unit =
    an[IllegalArgumentException] should be thrownBy notWorkingClient.connectorCreator().topicNames(Seq.empty)

  @Test
  def emptyTopicNames2(): Unit =
    an[IllegalArgumentException] should be thrownBy notWorkingClient.connectorCreator().topicNames(Seq(""))
}
