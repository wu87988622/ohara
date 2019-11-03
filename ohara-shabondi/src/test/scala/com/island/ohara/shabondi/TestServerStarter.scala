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

package com.island.ohara.shabondi

import com.island.ohara.common.rule.OharaTest
import com.island.ohara.shabondi.Model.{CmdArgs, HttpSink, HttpSource}
import org.junit.Test
import org.scalatest.Matchers

import scala.util.{Failure, Success}

class TestServerStarter extends OharaTest with Matchers {

  @Test
  def testParseArgs(): Unit = {
    ServerStarter.parseArgs(Array("--source", "9090")) shouldBe
      Success(CmdArgs(serverType = HttpSource, port = 9090))

    ServerStarter.parseArgs(Array("--sink", "9090")) shouldBe
      Success(CmdArgs(serverType = HttpSink, port = 9090))

    ServerStarter.parseArgs(Array()) shouldBe a[Failure[_]]
    ServerStarter.parseArgs(Array("--sink")) shouldBe a[Failure[_]]
    ServerStarter.parseArgs(Array("9090")) shouldBe a[Failure[_]]
  }

}
