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

package com.island.ohara.it

import java.time.Duration
import java.util.concurrent.TimeUnit

import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.CommonUtil
import org.junit.Rule
import org.junit.rules.Timeout

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class IntegrationTest extends OharaTest {
  @Rule def globalTimeout: Timeout = new Timeout(12, TimeUnit.MINUTES)

  protected def result[T](f: Future[T]): T = IntegrationTest.result(f)

  protected def await(f: () => Boolean): Unit = IntegrationTest.await(f)
}

object IntegrationTest {
  def result[T](f: Future[T]): T = Await.result(f, 300 seconds)

  def await(f: () => Boolean): Unit = CommonUtil.await(() => f(), Duration.ofSeconds(300))
}
