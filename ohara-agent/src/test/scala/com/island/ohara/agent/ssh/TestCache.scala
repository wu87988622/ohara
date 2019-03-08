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

package com.island.ohara.agent.ssh

import java.util.concurrent.TimeUnit

import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtil
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class TestCache extends SmallTest with Matchers {

  private[this] def result[T](f: Future[T]): T = Await.result(f, 30 seconds)
  @Test
  def nullDefault(): Unit = an[NullPointerException] should be thrownBy Cache.builder[String]().default(null)
  @Test
  def ignoreDefault(): Unit =
    an[NullPointerException] should be thrownBy Cache.builder[String]().fetcher(() => "abc").build()

  @Test
  def nullExpiredTime(): Unit = an[NullPointerException] should be thrownBy Cache.builder[String]().expiredTime(null)

  @Test
  def nullUpdater(): Unit = an[NullPointerException] should be thrownBy Cache.builder[String]().updater(null)

  @Test
  def testUpdate(): Unit = {
    val cache =
      Cache.builder[String]().expiredTime(2 seconds).fetcher(() => CommonUtil.randomString()).default("abc").build()
    try {
      val data = result(cache.get())
      data shouldBe result(cache.get())
      TimeUnit.SECONDS.sleep(3)
      data should not be result(cache.get())
    } finally cache.close()
  }

  @Test
  def testRequestToUpdate(): Unit = {
    val cache =
      Cache.builder[String]().expiredTime(1000 seconds).fetcher(() => CommonUtil.randomString()).default("abc").build()
    try {
      val data = result(cache.get())
      TimeUnit.SECONDS.sleep(3)
      data shouldBe result(cache.get())
      cache.requestUpdate() shouldBe true
      CommonUtil.await(() => result(cache.get()) != data, java.time.Duration.ofSeconds(10))
    } finally cache.close()
  }

  @Test
  def testLatest(): Unit = {
    val initialValue = CommonUtil.randomString()
    val latest = CommonUtil.randomString()
    val cache = Cache.builder[String]().expiredTime(1000 seconds).fetcher(() => latest).default(initialValue).build()
    try {
      result(cache.get()) shouldBe initialValue
      result(cache.latest()) shouldBe latest
    } finally cache.close()
  }

  @Test
  def testAutoRefresh(): Unit = {
    val initialValue = CommonUtil.randomString()
    val latest = CommonUtil.randomString()
    val cache = Cache.builder[String]().expiredTime(1000 seconds).fetcher(() => latest).default(initialValue).build()
    try {
      result(cache.get()) shouldBe initialValue
      CommonUtil.await(() => result(cache.latest()) == latest, java.time.Duration.ofSeconds(10))
    } finally cache.close()
  }

  @Test
  def testEmptyCache(): Unit = {
    val value = CommonUtil.randomString()
    val cache = Cache.empty(() => Future.successful(value))
    try {
      value shouldBe result(cache.get())
      value shouldBe result(cache.latest())
      TimeUnit.SECONDS.sleep(2)
      value shouldBe result(cache.get())
      value shouldBe result(cache.latest())
    } finally cache.close()
  }

  @Test
  def testClose(): Unit = {
    val cache = Cache.builder[String]().expiredTime(1000 seconds).fetcher(() => "aaa").default("Bbb").build()
    cache.close()
    an[IllegalStateException] should be thrownBy result(cache.get())
  }
}
