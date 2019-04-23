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

package com.island.ohara.configurator.store

import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorDescription
import com.island.ohara.client.configurator.v0.Data
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.rule.MediumTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

private[store] final case class SimpleData(id: String, name: String, lastModified: Long, kind: String) extends Data

class TestDataStore extends MediumTest with Matchers {

  private[this] val timeout = 10 seconds
  private[this] val store = DataStore(Store.inMemory(Serializer.STRING, Configurator.DATA_SERIALIZER))

  private def newTestData(id: String) = SimpleData(
    id = id,
    name = "name1",
    lastModified = CommonUtils.current(),
    kind = "kind1"
  )

  @After
  def tearDown(): Unit = Releasable.close(store)

  @Test
  def testAdd(): Unit = {
    val data = newTestData("abcd")
    Await.result(store.add(data), timeout)

    Await.result(store.exist[SimpleData](data.id), timeout) shouldBe true
    Await.result(store.exist[SimpleData]("12345"), timeout) shouldBe false
    Await.result(store.nonExist[SimpleData](data.id), timeout) shouldBe false
    Await.result(store.nonExist[SimpleData]("12345"), timeout) shouldBe true
  }

  @Test
  def testUpdate(): Unit = {
    val data = newTestData("abcd")
    Await.result(store.add(data), timeout)

    val data2 = (data: SimpleData) => Future.successful(data.copy(name = "name2"))

    Await.result(store.update(data.id, data2), timeout) should equal(data.copy(name = "name2"))
    an[NoSuchElementException] should be thrownBy Await.result(store.update("123", data2), timeout)
  }

  @Test
  def testList(): Unit = {
    Await.result(store.add(newTestData("abcd")), timeout)
    Await.result(store.add(newTestData("xyz")), timeout)

    store.size shouldBe 2
  }

  @Test
  def testRemove(): Unit = {
    val data1 = newTestData("abcd")
    val data2 = newTestData("xyz")

    Await.result(store.add(data1), timeout)
    Await.result(store.add(data2), timeout)
    store.size shouldBe 2

    an[NoSuchElementException] should be thrownBy Await.result(store.remove("1234"), 50 seconds)
    an[NoSuchElementException] should be thrownBy Await.result(store.remove[SimpleData]("1234"), 50 seconds)
    an[NoSuchElementException] should be thrownBy Await.result(store.remove[ConnectorDescription]("abcd"), 50 seconds)

    Await.result(store.remove[SimpleData]("abcd"), 50 seconds) shouldBe data1
    store.size shouldBe 1
  }

  @Test
  def testRaw(): Unit = {
    val data1 = newTestData("abcd")

    Await.result(store.add(data1), timeout)
    store.size shouldBe 1

    an[NoSuchElementException] should be thrownBy Await.result(store.remove("1234"), 50 seconds)
    an[NoSuchElementException] should be thrownBy Await.result(store.remove[SimpleData]("1234"), 50 seconds)
    an[NoSuchElementException] should be thrownBy Await.result(store.remove[ConnectorDescription]("abcd"), 50 seconds)

    Await.result(store.raw(), timeout).head.asInstanceOf[SimpleData] shouldBe data1
    Await.result(store.raw("abcd"), timeout).asInstanceOf[SimpleData] shouldBe data1
  }

}
