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
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global

class TestHdfsInfoRoute extends SmallTest with Matchers {
  private[this] val configurator = Configurator.builder().fake().build()

  private[this] val hdfsApi = HadoopApi.access().hostname(configurator.hostname).port(configurator.port)

  @Test
  def test(): Unit = {
    // test add
    result(hdfsApi.list).size shouldBe 0
    val response = result(hdfsApi.request().name(CommonUtils.randomString()).uri("file:////").create())
    result(hdfsApi.list).size shouldBe 1

    // test get
    response shouldBe result(hdfsApi.get(response.name))

    // test update
    val uri = CommonUtils.randomString()
    val newResponse = result(hdfsApi.request().name(response.name).uri(uri).update())
    result(hdfsApi.list).size shouldBe 1
    newResponse.uri shouldBe uri
    newResponse shouldBe result(hdfsApi.get(response.name))

    result(hdfsApi.delete(response.name))
    result(hdfsApi.list).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(hdfsApi.get("123"))
  }

  @Test
  def duplicateUpdate(): Unit = {
    val count = 10
    (0 until 10).foreach { index =>
      result(hdfsApi.request().name(index.toString).uri(index.toString).update())
    }
    result(hdfsApi.list).size shouldBe count
  }

  @Test
  def duplicateDelete(): Unit =
    (0 to 10).foreach(_ => result(hdfsApi.delete(CommonUtils.randomString(5))))

  @Test
  def testInvalidNameOnUpdate(): Unit = {
    val invalidStrings = Seq("a_", "a-", "a.", "a~")
    invalidStrings.foreach { invalidString =>
      an[IllegalArgumentException] should be thrownBy result(
        hdfsApi.request().name(invalidString).uri(CommonUtils.randomString()).update())
    }
  }

  @Test
  def testInvalidNameOnCreation(): Unit = {
    val invalidStrings = Seq("a_", "a-", "a.", "a~")
    invalidStrings.foreach { invalidString =>
      an[IllegalArgumentException] should be thrownBy result(
        hdfsApi.request().name(invalidString).uri(CommonUtils.randomString()).create())
    }
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
