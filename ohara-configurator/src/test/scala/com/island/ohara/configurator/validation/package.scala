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

package com.island.ohara.configurator

import com.island.ohara.client.configurator.v0.ValidationApi.ValidationReport
import org.scalatest.Matchers

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
package object validation extends Matchers {
  val NUMBER_OF_TASKS = 3
  private[this] def result[T](f: Future[T]): T = Await.result(f, 60 seconds)

  def assertFailure(f: Future[Seq[ValidationReport]]): Unit = {
    val reports = result(f)
    reports.size shouldBe NUMBER_OF_TASKS
    reports.isEmpty shouldBe false
    reports.map(_.message).foreach(_.nonEmpty shouldBe true)
    reports.foreach(report => withClue(report.message)(report.pass shouldBe false))
  }

  def assertSuccess(f: Future[Seq[ValidationReport]]): Unit = {
    val reports = result(f)
    reports.size shouldBe NUMBER_OF_TASKS
    reports.isEmpty shouldBe false
    reports.map(_.message).foreach(_.nonEmpty shouldBe true)
    reports.foreach(report => withClue(report.message)(report.pass shouldBe true))
  }
}
