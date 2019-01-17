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

package com.island.ohara.connector.hdfs

import com.island.ohara.common.rule.MediumTest
import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar

class TestFileUtils extends MediumTest with Matchers with MockitoSugar {

  @Test
  def testGetFileName(): Unit = {
    val result1 = FileUtils.offsetFileName("prefix", 1, 1000)
    result1 shouldBe "prefix-000000001-000001000" + FileUtils.FILENAME_ENDSWITH

    val result2 = FileUtils.offsetFileName("prefix", 222222, 999999999)
    result2 shouldBe "prefix-000222222-999999999" + FileUtils.FILENAME_ENDSWITH
  }

  @Test
  def testGetStopOffset(): Unit = {
    val fileNames = Iterator(
      "prefix-000000001-000000011" + FileUtils.FILENAME_ENDSWITH,
      "prefix-000001000-000001001" + FileUtils.FILENAME_ENDSWITH,
      "prefix-000000009-000000999" + FileUtils.FILENAME_ENDSWITH
    )

    FileUtils.getStopOffset(fileNames) shouldBe 1001
  }

  @Test
  def testGetStopZero(): Unit = {
    val fileNames = Iterator()
    FileUtils.getStopOffset(fileNames) shouldBe 0
  }

  @Test
  def testCheckFileNameFormat(): Unit = {
    FileUtils.checkFileNameFormat("preAfi123x-000000000-111111111.csv") shouldBe true
    FileUtils.checkFileNameFormat("preAfi123x-000000000-111111111") shouldBe false
    FileUtils.checkFileNameFormat("preAfi123x-000000000-111111111csv") shouldBe false
    FileUtils.checkFileNameFormat("preA-fi123x-000000000-111111111csv") shouldBe false
    FileUtils.checkFileNameFormat("preA_fi123x-000000000-111111111.csv") shouldBe false
    FileUtils.checkFileNameFormat("preAfi123x-000000000-111111111.txt") shouldBe true
  }

  @Test
  def testFileName(): Unit = {
    FileUtils.fileName("/home/user1/test.txt") shouldBe "test.txt"
    FileUtils.fileName("./test.txt") shouldBe "test.txt"
    FileUtils.fileName("test.txt") shouldBe "test.txt"
    FileUtils.fileName("test.txt.txt") shouldBe "test.txt.txt"
  }
}
