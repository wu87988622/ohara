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

package oharastream.ohara.client.configurator

import oharastream.ohara.common.rule.OharaTest
import oharastream.ohara.common.setting.ObjectKey
import oharastream.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.matchers.should.Matchers._

class TestVolumeApi extends OharaTest {
  @Test
  def testOnlyPath(): Unit =
    VolumeApi.access.request
      .path(CommonUtils.randomString())
      .creation

  @Test
  def testNameInCreation(): Unit =
    VolumeApi.access.request
      .name("ab")
      .path(CommonUtils.randomString())
      .creation
      .name shouldBe "ab"

  @Test
  def testGroupInCreation(): Unit =
    VolumeApi.access.request
      .group("ab")
      .path(CommonUtils.randomString())
      .creation
      .group shouldBe "ab"

  @Test
  def testKeyInCreation(): Unit = {
    val creation = VolumeApi.access.request
      .key(ObjectKey.of("g", "n"))
      .path(CommonUtils.randomString())
      .creation
    creation.group shouldBe "g"
    creation.name shouldBe "n"
  }

  @Test
  def testPathInCreation(): Unit =
    VolumeApi.access.request
      .path("a")
      .creation
      .path shouldBe "a"

  @Test
  def testPathInUpdating(): Unit =
    VolumeApi.access.request
      .path("a")
      .updating
      .path
      .get shouldBe "a"

  @Test
  def testDefaultTagsInCreation(): Unit =
    VolumeApi.access.request
      .path(CommonUtils.randomString())
      .creation
      .tags shouldBe Map.empty

  @Test
  def testDefaultTagsInUpdating(): Unit =
    VolumeApi.access.request.updating.tags shouldBe None
}
