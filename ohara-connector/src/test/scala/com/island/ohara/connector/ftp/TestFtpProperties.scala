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

package com.island.ohara.connector.ftp
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestFtpProperties extends SmallTest with Matchers {

  @Test
  def testFtpSinkProps(): Unit = {
    val props = FtpSinkProps(
      output = "/output",
      needHeader = false,
      user = "user",
      password = "pwd",
      hostname = "hostname",
      port = 123,
      encode = Some("UTF-8")
    )
    val copy = FtpSinkProps(props.toMap)
    copy shouldBe props
  }

  @Test
  def testFtpSinkTaskProps(): Unit = {
    val props = FtpSinkTaskProps(
      output = "/output",
      needHeader = false,
      user = "user",
      password = "pwd",
      hostname = "hostname",
      port = 123,
      encode = Some("UTF-8")
    )
    val copy = FtpSinkTaskProps(props.toMap)
    copy shouldBe props
  }

  @Test
  def testFtpSourceProps(): Unit = {
    val props = FtpSourceProps(
      inputFolder = "/input",
      completedFolder = "/output",
      errorFolder = "/error/",
      user = "user",
      password = "pwd",
      hostname = "hostname",
      port = 123,
      encode = Some("UTF-8")
    )
    val copy = FtpSourceProps(props.toMap)
    copy shouldBe props
  }

  @Test
  def testFtpSourceTaskProps(): Unit = {
    val props = FtpSourceTaskProps(
      hash = 123,
      total = 4,
      inputFolder = "/input",
      completedFolder = "/output",
      errorFolder = "/error/",
      user = "user",
      password = "pwd",
      hostname = "hostname",
      port = 123,
      encode = Some("UTF-8")
    )
    val copy = FtpSourceTaskProps(props.toMap)
    copy shouldBe props
  }

  @Test
  def testFtpSourcePropsWithNullOrEmptyEncode(): Unit = {
    val props1 = FtpSourceProps(
      inputFolder = "/input",
      completedFolder = "/output",
      errorFolder = "/error/",
      user = "user",
      password = "pwd",
      hostname = "hostname",
      port = 123,
      encode = None
    ).toMap
    props1.get("encode") shouldBe None

    val props2 = FtpSourceProps(
      inputFolder = "/input",
      completedFolder = "/output",
      errorFolder = "/error/",
      user = "user",
      password = "pwd",
      hostname = "hostname",
      port = 123,
      encode = Some("")
    ).toMap
    props2.get("encode") shouldBe None
  }

  @Test
  def testFtpSourceTaskPropsWithNullOrEmptyEncode(): Unit = {
    val props1 = FtpSourceTaskProps(
      hash = 123,
      total = 4,
      inputFolder = "/input",
      completedFolder = "/output",
      errorFolder = "/error/",
      user = "user",
      password = "pwd",
      hostname = "hostname",
      port = 123,
      encode = None
    ).toMap
    props1.get("encode") shouldBe None

    val props2 = FtpSourceTaskProps(
      hash = 123,
      total = 4,
      inputFolder = "/input",
      completedFolder = "/output",
      errorFolder = "/error/",
      user = "user",
      password = "pwd",
      hostname = "hostname",
      port = 123,
      encode = Some("")
    ).toMap
    props2.get("encode") shouldBe None
  }
}
