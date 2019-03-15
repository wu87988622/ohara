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
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorInfo
import com.island.ohara.client.configurator.v0.Data
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.store.{DataStore, Store}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
class TestConfiguratorStore extends MediumTest with Matchers {

  private[this] val timeout = 10 seconds
  private[this] val store = DataStore(Store.inMemory(Serializer.STRING, Configurator.DATA_SERIALIZER))

  @Test
  def testAdd(): Unit = {
    val s = ConnectorInfo(
      id = "asdad",
      name = "abc",
      className = "aaa.class",
      schema = Seq.empty,
      topics = Seq("abc"),
      numberOfTasks = 1,
      configs = Map.empty,
      state = None,
      error = None,
      workerClusterName = methodName(),
      lastModified = CommonUtils.current()
    )
    Await.result(store.add(s), timeout)

    Await.result(store.exist[Data](s.id), timeout) shouldBe true
    Await.result(store.nonExist[Data](s.id), timeout) shouldBe false
  }

  @Test
  def testUpdate(): Unit = {
    val s = ConnectorInfo(
      id = "asdad",
      name = "abc",
      className = "aaa.class",
      schema = Seq.empty,
      topics = Seq("abc"),
      numberOfTasks = 1,
      configs = Map.empty,
      state = None,
      error = None,
      workerClusterName = methodName(),
      lastModified = CommonUtils.current()
    )
    store.add(s)

    Await
      .result(store.update(s.id, (_: Data) => Future.successful(s.copy(name = "123"))), 10 seconds)
      .name shouldBe "123"

    an[NoSuchElementException] should be thrownBy Await
      .result(store.update("asdasdasd", (_: Data) => Future.successful(s.copy(id = "123"))), 10 seconds)
  }

  @Test
  def testList(): Unit = {
    val s = ConnectorInfo(
      id = "asdad",
      name = "abc",
      className = "aaa.class",
      schema = Seq.empty,
      topics = Seq("abc"),
      numberOfTasks = 1,
      configs = Map.empty,
      state = None,
      error = None,
      workerClusterName = methodName(),
      lastModified = CommonUtils.current()
    )
    store.add(s)

    store.size shouldBe 1

    Await.result(store.raw(), 10 seconds).head.asInstanceOf[ConnectorInfo] shouldBe s

    Await.result(store.raw(s.id), 10 seconds).asInstanceOf[ConnectorInfo] shouldBe s
  }

  @Test
  def testRemove(): Unit = {
    val s = ConnectorInfo(
      id = "asdad",
      name = "abc",
      className = "aaa.class",
      schema = Seq.empty,
      topics = Seq("abc"),
      numberOfTasks = 1,
      configs = Map.empty,
      state = None,
      error = None,
      workerClusterName = methodName(),
      lastModified = CommonUtils.current()
    )
    store.add(s)

    store.size shouldBe 1

    an[NoSuchElementException] should be thrownBy Await.result(store.remove("asdasd"), 50 seconds)
    an[NoSuchElementException] should be thrownBy Await.result(store.remove[ConnectorInfo]("asdasd"), 50 seconds)

    Await.result(store.remove[ConnectorInfo](s.id), 50 seconds) shouldBe s

    store.size shouldBe 0
  }

  @After
  def tearDown(): Unit = Releasable.close(store)

}
