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

import com.island.ohara.client.configurator.v0.ShabondiApi
import com.island.ohara.client.configurator.v0.ShabondiApi.ShabondiProperty
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.configurator.Configurator
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestShabondiRoute extends SmallTest with Matchers {

  private val configurator = Configurator.builder().fake().build()
  private val access = ShabondiApi.access().hostname(configurator.hostname).port(configurator.port)

  private def awaitResult[T](f: Future[T]): T = Await.result(f, 20 seconds)

  @Test
  def testAdd(): Unit = {
    val desc1: ShabondiApi.ShabondiDescription = awaitResult(access.add())

    desc1.id.size should be > 0
    desc1.to should be(Seq.empty)
    desc1.state should be(None)
    desc1.port should be(-1)
    desc1.instances should be(1)
  }

  @Test
  def testDelete(): Unit = {
    val desc1 = awaitResult(access.add())
    awaitResult(access.delete(desc1.id))

    an[IllegalArgumentException] should be thrownBy awaitResult(access.delete("12345"))
  }

  @Test
  def testGetProperty(): Unit = {
    val desc1 = awaitResult(access.add())
    val desc2 = awaitResult(access.getProperty(desc1.id))

    desc1.id should be(desc2.id)
    desc1.name should be(desc2.name)
    desc1.to should be(desc2.to)
    desc1.lastModified should be(desc2.lastModified)
    desc1.state should be(desc2.state)
    desc1.port should be(desc2.port)
    desc1.instances should be(desc2.instances)
  }

  @Test
  def tesUpdateProperty(): Unit = {
    val desc1 = awaitResult(access.add())
    val property = ShabondiProperty(Some("xyz"), Some(Seq("topic1")), Some(250))
    val desc2 = awaitResult(access.updateProperty(desc1.id, property))

    desc2.id should be(desc1.id)
    desc2.name should be("xyz")
    desc2.to should be(Seq("topic1"))
    desc2.port should be(250)
    desc2.lastModified should not be (desc1.lastModified)

    val desc3 = awaitResult(access.getProperty(desc1.id))

    desc3.id should be(desc1.id)
    desc3.name should be("xyz")
    desc3.to should be(Seq("topic1"))
    desc3.port should be(250)
    desc3.lastModified should not be (desc1.lastModified)
  }
}
