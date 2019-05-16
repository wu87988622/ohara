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

import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers

class TestConfiguratorBuilder extends SmallTest with Matchers {
  @Test
  def nullHomeFolder(): Unit = an[NullPointerException] should be thrownBy Configurator.builder().homeFolder(null)

  @Test
  def emptyHomeFolder(): Unit = an[IllegalArgumentException] should be thrownBy Configurator.builder().homeFolder("")

  @Test
  def fileToHomeFolder(): Unit = an[IllegalArgumentException] should be thrownBy Configurator
    .builder()
    .homeFolder(CommonUtils.createTempFile(methodName()).getCanonicalPath)

  @Test
  def autoMkdirForHomeFolder(): Unit = {
    val folder = CommonUtils.createTempFolder(methodName())
    folder.delete() shouldBe true
    folder.exists() shouldBe false
    Configurator.builder().homeFolder(folder.getCanonicalPath)
    folder.exists() shouldBe true
  }
}
