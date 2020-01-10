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

package com.island.ohara.configurator.route

import com.island.ohara.common.rule.OharaTest
import com.typesafe.scalalogging.Logger
import org.junit.Test
import org.scalatest.Matchers._

import scala.io.{Codec, Source}

class TestApiUrl extends OharaTest {
  private[this] val log = Logger(classOf[TestApiUrl])

  @Test
  def testDocumentApiUrl(): Unit = {
    log.info("defaultCharsetCodec={}", Codec.defaultCharsetCodec)
    val html = Source.fromURL(com.island.ohara.configurator.route.apiUrl)(Codec.UTF8).mkString

    html.contains("""<p class="caption"><span class="caption-text">REST APIs</span></p>""") should ===(true)
  }
}
