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

package com.island.ohara.demo

import com.island.ohara.common.rule.LargeTest
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
@deprecated("embedded services are deprecated. We all should love docker, shouldn't we?", "0.2")
class TestRunBackendWithTtl extends LargeTest with Matchers {

  @Test
  def testTtl(): Unit = {
    val f = Future {
      Backend.main(Array(Backend.TTL_KEY, "1"))
    }
    // we have to wait all service to be closed so 180 seconds is a safe limit.
    Await.result(f, 180 seconds)
  }
}
