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

package com.island.ohara.configurator
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorDescription
import com.island.ohara.client.configurator.v0.Data
import com.island.ohara.client.configurator.v0.PipelineApi.Metrics
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.store.{DataStore, Store}
import org.junit.{After, Test}
import org.scalatest.Matchers
import spray.json.JsString

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestConfiguratorStore extends MediumTest with Matchers {

  private[this] val timeout = 10 seconds
  private[this] val store = DataStore(Store.inMemory(Serializer.STRING, Configurator.DATA_SERIALIZER))

  @Test
  def testAdd(): Unit = {
    val s = ConnectorDescription(
      id = "asdad",
      settings = Map.empty,
      state = None,
      error = None,
      metrics = Metrics(Seq.empty),
      lastModified = CommonUtils.current()
    )
    Await.result(store.add(s), timeout)

    Await.result(store.exist[Data](s.id), timeout) shouldBe true
    Await.result(store.exist[Data]("12345"), timeout) shouldBe false
    Await.result(store.nonExist[Data](s.id), timeout) shouldBe false
    Await.result(store.nonExist[Data]("12345"), timeout) shouldBe true
  }

  @Test
  def testUpdate(): Unit = {
    val s = ConnectorDescription(
      id = "asdad",
      settings = Map.empty,
      state = None,
      error = None,
      metrics = Metrics(Seq.empty),
      lastModified = CommonUtils.current()
    )
    store.add(s)

    Await
      .result(store.update(s.id, (_: Data) => Future.successful(s.copy(settings = Map("a" -> JsString("b"))))),
              10 seconds)
      .settings shouldBe Map("a" -> JsString("b"))

    an[NoSuchElementException] should be thrownBy Await
      .result(store.update("asdasdasd", (_: Data) => Future.successful(s.copy(id = "123"))), 10 seconds)
  }

  @Test
  def testList(): Unit = {
    val s = ConnectorDescription(
      id = "asdad",
      settings = Map.empty,
      state = None,
      error = None,
      metrics = Metrics(Seq.empty),
      lastModified = CommonUtils.current()
    )
    store.add(s)

    store.size shouldBe 1

    Await.result(store.raw(), 10 seconds).head.asInstanceOf[ConnectorDescription] shouldBe s

    Await.result(store.raw(s.id), 10 seconds).asInstanceOf[ConnectorDescription] shouldBe s
  }

  @Test
  def testRemove(): Unit = {
    val s = ConnectorDescription(
      id = "asdad",
      settings = Map.empty,
      state = None,
      error = None,
      metrics = Metrics(Seq.empty),
      lastModified = CommonUtils.current()
    )
    store.add(s)

    store.size shouldBe 1

    an[NoSuchElementException] should be thrownBy Await.result(store.remove("asdasd"), 50 seconds)
    an[NoSuchElementException] should be thrownBy Await.result(store.remove[ConnectorDescription]("asdasd"), 50 seconds)

    Await.result(store.remove[ConnectorDescription](s.id), 50 seconds) shouldBe s

    store.size shouldBe 0
  }

  @After
  def tearDown(): Unit = Releasable.close(store)

}
