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

import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import com.island.ohara.common.rule.OharaTest
import org.junit.Test
import org.scalatest.Matchers._

class TestWebServer extends OharaTest {
  import HttpMethods._
  import ShabondiRouteTest._
  import com.island.ohara.shabondi.Model._

  @Test
  def testSourceRoute(): Unit = {
    val webServer = new WebServer(HttpSource)

    val request = HttpRequest(uri = "/v0", method = POST)

    request ~> webServer.sourceRoute ~> check {
      entityAs[String] should ===("sourceRoute OK")
    }
  }

  @Test
  def testSinkRoute(): Unit = {
    val webServer = new WebServer(HttpSink)

    val request = HttpRequest(uri = "/v0", method = POST)

    request ~> webServer.sinkRoute ~> check {
      entityAs[String] should ===("sinkRoute OK")
    }
  }
}
