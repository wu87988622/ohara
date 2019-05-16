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

import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
abstract class BasicTestStore extends SmallTest with Matchers {

  protected val store: Store[String, String]

  protected def result[T](f: Future[T]): T = Await.result(f, 20 seconds)

  @Test
  def testGetter(): Unit = {
    val key = CommonUtils.randomString()
    val value = CommonUtils.randomString()
    result(store.add(key, value)) shouldBe value
    result(store.get(key)) shouldBe Some(value)
  }

  @Test
  def testValue(): Unit = {
    val key = CommonUtils.randomString()
    val value = CommonUtils.randomString()
    an[NoSuchElementException] should be thrownBy result(store.value(key))
    result(store.add(key, value)) shouldBe value
    result(store.value(key)) shouldBe value
  }

  @Test
  def testMultiPut(): Unit = {
    store.size shouldBe 0
    result(store.add(CommonUtils.randomString(), CommonUtils.randomString()))
    store.size shouldBe 1
    result(store.add(CommonUtils.randomString(), CommonUtils.randomString()))
    store.size shouldBe 2
    result(store.add(CommonUtils.randomString(), CommonUtils.randomString()))
    store.size shouldBe 3
  }

  @Test
  def testDelete(): Unit = {
    val key = CommonUtils.randomString()
    val value = CommonUtils.randomString()
    result(store.add(key, value)) shouldBe value
    result(store.get(key)) shouldBe Some(value)
    result(store.remove(key)) shouldBe true
    store.size shouldBe 0
  }

  @Test
  def testDuplicateAdd(): Unit = {
    val key = CommonUtils.randomString()
    val value0 = CommonUtils.randomString()
    val value1 = CommonUtils.randomString()
    result(store.add(key, value0)) shouldBe value0
    store.size shouldBe 1
    an[IllegalStateException] should be thrownBy result(store.add(key, value1))
    store.size shouldBe 1
    result(store.add(CommonUtils.randomString(), value1))
    store.size shouldBe 2
    result(store.values()).size shouldBe store.size
  }

  @Test
  def testUpdate(): Unit = {
    an[NoSuchElementException] should be thrownBy result(
      store.update(CommonUtils.randomString(), _ => Future.successful(CommonUtils.randomString())))
    val key = CommonUtils.randomString()
    val value0 = CommonUtils.randomString()
    val value1 = CommonUtils.randomString()
    result(store.add(key, value0)) shouldBe value0
    store.size shouldBe 1
    result(store.update(key, v => {
      v shouldBe value0
      Future.successful(value1)
    })) shouldBe value1
    store.size shouldBe 1
  }

  @Test
  def testValues(): Unit = {
    val key0 = CommonUtils.randomString()
    val value0 = CommonUtils.randomString()
    val key1 = CommonUtils.randomString()
    val value1 = CommonUtils.randomString()
    result(store.add(key0, value0)) shouldBe value0
    result(store.add(key1, value1)) shouldBe value1
    result(store.values()).size shouldBe 2
    result(store.values())(key0) shouldBe value0
    result(store.values())(key1) shouldBe value1
  }

  @Test
  def testExist(): Unit = {
    val key = CommonUtils.randomString()
    val value = CommonUtils.randomString()
    result(store.exist(key)) shouldBe false
    result(store.add(key, value)) shouldBe value
    result(store.exist(key)) shouldBe true
  }

  @After
  def tearDown(): Unit = Releasable.close(store)
}
