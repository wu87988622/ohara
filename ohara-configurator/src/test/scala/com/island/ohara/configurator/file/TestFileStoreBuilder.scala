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

package com.island.ohara.configurator.file

import com.island.ohara.common.rule.OharaTest
import org.junit.Test
import org.scalatest.Matchers

class TestFileStoreBuilder extends OharaTest with Matchers {

  @Test
  def nullHomeFolder(): Unit = an[NullPointerException] should be thrownBy FileStore.builder.homeFolder(null)

  @Test
  def emptyHomeFolder(): Unit = an[IllegalArgumentException] should be thrownBy FileStore.builder.homeFolder("")

  @Test
  def nullHostname(): Unit = an[NullPointerException] should be thrownBy FileStore.builder.hostname(null)

  @Test
  def emptyHostname(): Unit = an[IllegalArgumentException] should be thrownBy FileStore.builder.hostname("")

  @Test
  def navigatePort(): Unit = an[IllegalArgumentException] should be thrownBy FileStore.builder.port(-1)

  @Test
  def nullAcceptedExtensions(): Unit =
    an[NullPointerException] should be thrownBy FileStore.builder.acceptedExtensions(null)

  @Test
  def emptyAcceptedExtensions(): Unit =
    an[IllegalArgumentException] should be thrownBy FileStore.builder.acceptedExtensions(Set.empty)

  @Test
  def dotIsIllegalInAcceptedExtensions(): Unit =
    an[IllegalArgumentException] should be thrownBy FileStore.builder.acceptedExtensions(Set(".aaa"))
}
