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
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestRocksDataStore extends SmallTest with Matchers {
  private[this] val store: DataStore = DataStore()

  private[this] def createData(_name: String) = SimpleData(
    name = _name,
    lastModified = CommonUtils.current(),
    kind = _name
  )

  private[this] def result[T](f: Future[T]): T = Await.result(f, 20 seconds)

  private[this] def createData: SimpleData = createData(CommonUtils.randomString())

  @Test
  def testReopen(): Unit = {
    val folder = CommonUtils.createTempFolder(methodName())

    val key0 = CommonUtils.randomString()
    val value0 = createData(key0)
    val key1 = CommonUtils.randomString()
    val value1 = createData(key1)

    val s0 = DataStore.builder.persistentFolder(folder.getCanonicalPath).build()
    try {
      s0.numberOfTypes() shouldBe 0
      result(s0.addIfAbsent(value0))
      result(s0.addIfAbsent(value1))
      s0.size() shouldBe 2
      s0.numberOfTypes() shouldBe 1
    } finally s0.close()

    val s1 = DataStore.builder.persistentFolder(folder.getCanonicalPath).build()
    try {
      s1.numberOfTypes() shouldBe 1
      s1.size() shouldBe 2
      result(s1.value[SimpleData](key0)) shouldBe value0
      result(s1.value[SimpleData](key1)) shouldBe value1
    } finally s1.close()
  }

  @Test
  def testGetter(): Unit = {
    val key = CommonUtils.randomString()
    val value = createData(key)
    result(store.addIfAbsent(value)) shouldBe value
    result(store.get[SimpleData](key)) shouldBe Some(value)
  }

  @Test
  def testValue(): Unit = {
    val key = CommonUtils.randomString()
    val value = createData(key)
    an[NoSuchElementException] should be thrownBy result(store.value(key))
    result(store.addIfAbsent(value)) shouldBe value
    result(store.value[SimpleData](key)) shouldBe value
  }

  @Test
  def testMultiPut(): Unit = {
    store.size shouldBe 0
    result(store.addIfAbsent(createData))
    store.size shouldBe 1
    result(store.addIfAbsent(createData))
    store.size shouldBe 2
    result(store.addIfAbsent(createData))
    store.size shouldBe 3
  }

  @Test
  def testDelete(): Unit = {
    val key = CommonUtils.randomString()
    val value = createData(key)
    result(store.addIfAbsent(value)) shouldBe value
    result(store.get[SimpleData](key)) shouldBe Some(value)
    result(store.remove[SimpleData](key)) shouldBe true
    store.size shouldBe 0
  }

  @Test
  def testDuplicateAddIfAbsent(): Unit = {
    val value0 = createData
    val value1 = createData(value0.name)
    result(store.addIfAbsent(value0)) shouldBe value0
    store.size shouldBe 1
    an[IllegalStateException] should be thrownBy result(store.addIfAbsent(value1))
    store.size shouldBe 1
    result(store.addIfAbsent(createData))
    store.size shouldBe 2
    result(store.raws()).size shouldBe store.size
  }

  @Test
  def testDuplicateAdd(): Unit = {
    val value0 = createData
    (0 until 10).foreach(_ => result(store.add(value0.name, value0)))
  }

  @Test
  def testUpdate0(): Unit = {
    an[NoSuchElementException] should be thrownBy result(
      store.addIfPresent[SimpleData](CommonUtils.randomString(), _ => Future.successful(createData)))
    val key = CommonUtils.randomString()
    val value0 = createData(key)
    val value1 = createData(value0.name)
    result(store.addIfAbsent(value0)) shouldBe value0
    store.size shouldBe 1
    result(store.addIfPresent[SimpleData](key, v => {
      v shouldBe value0
      Future.successful(value1)
    })) shouldBe value1
    store.size shouldBe 1

    val value2 = createData
    result(store.addIfPresent[SimpleData](key, v => {
      v shouldBe value1
      Future.successful(value2)
    })) shouldBe value2
    store.size shouldBe 2
  }

  @Test
  def testValues(): Unit = {
    val value0 = createData
    val value1 = createData
    result(store.addIfAbsent(value0)) shouldBe value0
    result(store.addIfAbsent(value1)) shouldBe value1
    result(store.values[SimpleData]()).size shouldBe 2
    result(store.values[SimpleData]()).contains(value0) shouldBe true
    result(store.values[SimpleData]()).contains(value1) shouldBe true
  }

  @Test
  def testExist(): Unit = {
    val key = CommonUtils.randomString()
    val value = createData(key)
    result(store.exist(key)) shouldBe false
    result(store.addIfAbsent(value)) shouldBe value
    result(store.exist(key)) shouldBe false
    result(store.exist[SimpleData](key)) shouldBe true
  }

  @Test
  def testAdd(): Unit = {
    val data = createData("abcd")
    result(store.addIfAbsent(data))

    result(store.exist[SimpleData](data.name)) shouldBe true
    result(store.exist[SimpleData]("12345")) shouldBe false
    result(store.nonExist[SimpleData](data.name)) shouldBe false
    result(store.nonExist[SimpleData]("12345")) shouldBe true
  }

  @Test
  def testUpdate(): Unit = {
    val data = createData("abcd")
    result(store.addIfAbsent(data))

    val data2 = (data: SimpleData) => Future.successful(data.copy(name = "name2"))

    result(store.addIfPresent(data.name, data2)) should equal(data.copy(name = "name2"))
    an[NoSuchElementException] should be thrownBy result(store.addIfPresent("123", data2))
  }

  @Test
  def testList(): Unit = {
    result(store.addIfAbsent(createData("abcd")))
    result(store.addIfAbsent(createData("xyz")))

    store.size shouldBe 2
  }

  @Test
  def testRemove(): Unit = {
    val data1 = createData("abcd")
    val data2 = createData("xyz")

    result(store.addIfAbsent(data1))
    result(store.addIfAbsent(data2))
    store.size shouldBe 2

    result(store.remove("1234")) shouldBe false
    result(store.remove[SimpleData]("1234")) shouldBe false
    result(store.remove[ConnectorDescription]("abcd")) shouldBe false

    result(store.remove[SimpleData]("abcd")) shouldBe true
    store.size shouldBe 1
  }

  @Test
  def testRaw(): Unit = {
    val data1 = createData("abcd")

    result(store.addIfAbsent(data1))
    store.size shouldBe 1

    result(store.remove("1234")) shouldBe false
    result(store.remove[SimpleData]("1234")) shouldBe false
    result(store.remove[ConnectorDescription]("abcd")) shouldBe false

    result(store.raws()).head.asInstanceOf[SimpleData] shouldBe data1
    result(store.raws("abcd")).head.asInstanceOf[SimpleData] shouldBe data1
  }

  @Test
  def testAccessClosedStore(): Unit = {
    val store2: DataStore = DataStore()
    store2.close()
    an[RuntimeException] should be thrownBy store2.size()
    an[RuntimeException] should be thrownBy store2.numberOfTypes()
    an[RuntimeException] should be thrownBy result(store2.get[SimpleData](CommonUtils.randomString(10)))
    an[RuntimeException] should be thrownBy result(store2.add[SimpleData](CommonUtils.randomString(10), createData))
    an[RuntimeException] should be thrownBy result(store2.exist[SimpleData](CommonUtils.randomString(10)))
    an[RuntimeException] should be thrownBy result(
      store2.addIfAbsent[SimpleData](CommonUtils.randomString(10), createData))
    an[RuntimeException] should be thrownBy result(
      store2.addIfPresent[SimpleData](CommonUtils.randomString(10), _ => Future.successful(createData)))
    an[RuntimeException] should be thrownBy result(store2.raw(CommonUtils.randomString(10)))
    an[RuntimeException] should be thrownBy result(store2.raws(CommonUtils.randomString(10)))
    an[RuntimeException] should be thrownBy result(store2.raws())
    an[RuntimeException] should be thrownBy result(store2.value[SimpleData](CommonUtils.randomString(10)))
    an[RuntimeException] should be thrownBy result(store2.values[SimpleData]())
    an[RuntimeException] should be thrownBy result(store2.remove[SimpleData](CommonUtils.randomString(10)))
  }

  @After
  def tearDown(): Unit = Releasable.close(store)
}
