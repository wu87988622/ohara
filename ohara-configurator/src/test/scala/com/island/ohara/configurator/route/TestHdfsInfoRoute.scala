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

import com.island.ohara.client.configurator.v0.HadoopApi
import com.island.ohara.client.configurator.v0.HadoopApi.{HdfsInfo, Request}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global

class TestHdfsInfoRoute extends SmallTest with Matchers {
  private[this] val configurator = Configurator.builder.fake().build()

  private[this] val hdfsApi = HadoopApi.access.hostname(configurator.hostname).port(configurator.port)

  @Test
  def test(): Unit = {
    // test add
    val name = CommonUtils.randomString()
    val uri = CommonUtils.randomString()
    val response = result(hdfsApi.request.name(name).uri(uri).create())
    response.name shouldBe name
    response.uri shouldBe uri

    // test get
    response shouldBe result(hdfsApi.get(response.name))

    // test update
    val uri2 = CommonUtils.randomString()
    val newResponse = result(hdfsApi.request.name(response.name).uri(uri2).update())
    result(hdfsApi.list()).size shouldBe 1
    newResponse.name shouldBe name
    newResponse.uri shouldBe uri2
    newResponse shouldBe result(hdfsApi.get(response.name))

    result(hdfsApi.delete(response.name))
    result(hdfsApi.list()).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(hdfsApi.get("123"))
  }

  @Test
  def duplicateUpdate(): Unit = {
    val count = 10
    (0 until 10).foreach { index =>
      result(hdfsApi.request.name(index.toString).uri(index.toString).update())
    }
    result(hdfsApi.list()).size shouldBe count
  }

  @Test
  def duplicateDelete(): Unit =
    (0 to 10).foreach(_ => result(hdfsApi.delete(CommonUtils.randomString(5))))

  @Test
  def testInvalidNameOnUpdate(): Unit = {
    val invalidStrings = Seq("a@", "a=", "a\\", "a~", "a//")
    invalidStrings.foreach { invalidString =>
      an[IllegalArgumentException] should be thrownBy result(
        hdfsApi.request.name(invalidString).uri(CommonUtils.randomString()).update())
    }
  }

  @Test
  def testInvalidNameOnCreation(): Unit = {
    val invalidStrings = Seq("a@", "a=", "a\\", "a~", "a//")
    invalidStrings.foreach { invalidString =>
      an[IllegalArgumentException] should be thrownBy result(
        hdfsApi.request.name(invalidString).uri(CommonUtils.randomString()).create())
    }
  }

  @Test
  def testUpdateUri(): Unit = {
    val uri = CommonUtils.randomString()
    updatePartOfField(_.uri(uri), _.copy(uri = uri))
  }

  private[this] def updatePartOfField(req: Request => Request, _expected: HdfsInfo => HdfsInfo): Unit = {
    val previous = result(hdfsApi.request.name(CommonUtils.randomString()).uri(CommonUtils.randomString()).update())
    val updated = result(req(hdfsApi.request.name(previous.name)).update())
    val expected = _expected(previous)
    updated.name shouldBe expected.name
    updated.uri shouldBe expected.uri
  }

  @Test
  def updateTags(): Unit = {
    val tags = Set(CommonUtils.randomString(10), CommonUtils.randomString(10))
    val hdfsDesc = result(hdfsApi.request.uri("password").tags(tags).create())
    hdfsDesc.tags shouldBe tags

    val tags2 = Set(CommonUtils.randomString(10), CommonUtils.randomString(10))
    val hdfsDesc2 = result(hdfsApi.request.name(hdfsDesc.name).tags(tags2).update())
    hdfsDesc2.tags shouldBe tags2

    val hdfsDesc3 = result(hdfsApi.request.name(hdfsDesc.name).update())
    hdfsDesc3.tags shouldBe tags2

    val hdfsDesc4 = result(hdfsApi.request.name(hdfsDesc.name).tags(Set.empty).update())
    hdfsDesc4.tags shouldBe Set.empty
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
