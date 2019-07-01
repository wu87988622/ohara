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

package com.island.ohara.client.configurator.v0

import java.net.URL

import com.island.ohara.client.configurator.v0.JarApi.JarInfo
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
class TestJarApi extends SmallTest with Matchers {

  @Test
  def testId(): Unit = {
    val jarInfo = JarInfo(
      name = CommonUtils.randomString(),
      group = CommonUtils.randomString(),
      size = 1L,
      url = new URL("file://"),
      lastModified = CommonUtils.current()
    )
    jarInfo.id shouldBe jarInfo.name
  }

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy JarApi.access.request.newName("")

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy JarApi.access.request.newName(null)

  @Test
  def emptyGroup(): Unit = an[IllegalArgumentException] should be thrownBy JarApi.access.request.group("")

  @Test
  def nullGroup(): Unit = an[NullPointerException] should be thrownBy JarApi.access.request.group(null)
}
