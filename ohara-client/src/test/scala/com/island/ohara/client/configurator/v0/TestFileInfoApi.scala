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

import java.io.File
import java.net.URL

import com.island.ohara.client.configurator.v0.FileInfoApi.FileInfo
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers._
import spray.json.JsString

import scala.concurrent.ExecutionContext.Implicits.global
class TestFileInfoApi extends OharaTest {

  private[this] def access: FileInfoApi.Access = FileInfoApi.access.hostname(CommonUtils.hostname()).port(22)

  @Test
  def nullKeyInGet(): Unit =
    an[NullPointerException] should be thrownBy access.get(null)

  @Test
  def nullKeyInDelete(): Unit =
    an[NullPointerException] should be thrownBy access.delete(null)

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy access.request.name("")

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy access.request.name(null)

  @Test
  def emptyGroup(): Unit = an[IllegalArgumentException] should be thrownBy access.request.group("")

  @Test
  def nullGroup(): Unit = an[NullPointerException] should be thrownBy access.request.group(null)

  @Test
  def nullFile(): Unit = an[NullPointerException] should be thrownBy access.request.file(null)

  @Test
  def nonexistentFile(): Unit =
    an[IllegalArgumentException] should be thrownBy access.request.file(new File(CommonUtils.randomString(5)))

  @Test
  def nullTags(): Unit = an[NullPointerException] should be thrownBy access.request.tags(null)

  @Test
  def emptyTags(): Unit = access.request.tags(Map.empty)

  @Test
  def bytesMustBeEmptyAfterSerialization(): Unit = {
    val bytes = CommonUtils.randomString().getBytes()
    val fileInfo = new FileInfo(
      group = CommonUtils.randomString(),
      name = CommonUtils.randomString(),
      lastModified = CommonUtils.current(),
      bytes = bytes,
      url = new URL("http://localhost:1345/v0"),
      tags = Map("a" -> JsString("b"))
    )

    val copy = FileInfoApi.FILE_INFO_JSON_FORMAT.read(FileInfoApi.FILE_INFO_JSON_FORMAT.write(fileInfo))
    copy.group shouldBe fileInfo.group
    copy.name shouldBe fileInfo.name
    copy.lastModified shouldBe fileInfo.lastModified
    copy.bytes shouldBe Array.empty
    copy.url shouldBe fileInfo.url
    copy.tags shouldBe fileInfo.tags
  }
}
