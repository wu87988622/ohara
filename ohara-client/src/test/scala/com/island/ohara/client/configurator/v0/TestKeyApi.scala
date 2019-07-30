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

import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json._

class TestKeyApi extends SmallTest with Matchers {

  @Test
  def testObjectKey(): Unit = {
    val name = CommonUtils.randomString()
    val objectKey = OBJECT_KEY_FORMAT.read(s"""
                                              |
                                              | {
                                              |   "name": "$name"
                                              | }
                                              |""".stripMargin.parseJson)
    objectKey.group() shouldBe Data.GROUP_DEFAULT
    objectKey.name() shouldBe name
  }

  @Test
  def testEmptyNameInObjectKey(): Unit = an[DeserializationException] should be thrownBy
    OBJECT_KEY_FORMAT.read(s"""
                              |
                              | {
                              |   "name": ""
                              | }
                              |""".stripMargin.parseJson)

  @Test
  def testEmptyGroupInObjectKey(): Unit = an[DeserializationException] should be thrownBy
    OBJECT_KEY_FORMAT.read(s"""
                              |
                              | {
                              |   "group": "g0",
                              |   "name": ""
                              | }
                              |""".stripMargin.parseJson)

  @Test
  def testTopicKey(): Unit = {
    val name = CommonUtils.randomString()
    val topicKey = TOPIC_KEY_FORMAT.read(s"""
                                              |
                                              | {
                                              |   "name": "$name"
                                              | }
                                              |""".stripMargin.parseJson)
    topicKey.group() shouldBe Data.GROUP_DEFAULT
    topicKey.name() shouldBe name
  }

  @Test
  def testEmptyNameInTopicKey(): Unit = an[DeserializationException] should be thrownBy
    TOPIC_KEY_FORMAT.read(s"""
                              |
                              | {
                              |   "name": ""
                              | }
                              |""".stripMargin.parseJson)

  @Test
  def testEmptyGroupInTopicKey(): Unit = an[DeserializationException] should be thrownBy
    TOPIC_KEY_FORMAT.read(s"""
                              |
                              | {
                              |   "group": "g0",
                              |   "name": ""
                              | }
                              |""".stripMargin.parseJson)
}
