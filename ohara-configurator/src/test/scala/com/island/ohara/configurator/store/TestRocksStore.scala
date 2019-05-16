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

import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.CommonUtils
import org.junit.Test

import scala.concurrent.ExecutionContext.Implicits.global
class TestRocksStore extends BasicTestStore {
  override protected val store: Store[String, String] = Store
    .builder[String, String]()
    .keySerializer(Serializer.STRING)
    .valueSerializer(Serializer.STRING)
    .persistentFolder(CommonUtils.createTempFolder("TestRocksStore").getCanonicalPath)
    .build()

  @Test
  def testReopen(): Unit = {
    val folder = CommonUtils.createTempFolder(methodName())

    val key0 = CommonUtils.randomString()
    val value0 = CommonUtils.randomString()
    val key1 = CommonUtils.randomString()
    val value1 = CommonUtils.randomString()

    val s0 = Store
      .builder[String, String]()
      .keySerializer(Serializer.STRING)
      .valueSerializer(Serializer.STRING)
      .persistentFolder(folder.getCanonicalPath)
      .build()
    try {
      result(s0.add(key0, value0))
      result(s0.add(key1, value1))
      s0.size shouldBe 2
    } finally s0.close()

    val s1 = Store
      .builder[String, String]()
      .keySerializer(Serializer.STRING)
      .valueSerializer(Serializer.STRING)
      .persistentFolder(folder.getCanonicalPath)
      .build()
    try {
      s1.size shouldBe 2
      result(s1.value(key0)) shouldBe value0
      result(s1.value(key1)) shouldBe value1
    } finally s1.close()
  }
}
