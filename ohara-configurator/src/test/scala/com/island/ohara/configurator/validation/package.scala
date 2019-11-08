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

import com.island.ohara.client.configurator.v0.ValidationApi.{RdbValidationReport, ValidationReport}
import org.scalatest.Matchers._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
package object validation {
  val NUMBER_OF_TASKS = 3
  def result[T](f: Future[T]): T = Await.result(f, 60 seconds)

  def assertFailure(reports: Seq[ValidationReport]): Unit = {
    reports.size should be >= NUMBER_OF_TASKS
    reports.size should not be 0
    reports.map(_.message).foreach(_.length should not be 0)
    reports.foreach(report => withClue(report.message)(report.pass shouldBe false))
  }

  def assertSuccess(reports: Seq[ValidationReport]): Unit = {
    reports.size should be >= NUMBER_OF_TASKS
    reports.size should not be 0
    reports.map(_.message).foreach(_.length should not be 0)
    reports.foreach(report => withClue(report.message)(report.pass shouldBe true))
  }

  def assertJdbcSuccess(reports: Seq[RdbValidationReport]): Unit = {
    reports.size should be >= NUMBER_OF_TASKS
    reports.size should not be 0
    reports.map(_.message).foreach(_.length should not be 0)
    reports.foreach(report => withClue(report.message)(report.pass shouldBe true))
    reports.foreach(_.rdbInfo.get.tables.size should not be 0)
  }

  def assertJdbcFailure(reports: Seq[RdbValidationReport]): Unit = {
    reports.size should be >= NUMBER_OF_TASKS
    reports.size should not be 0
    reports.map(_.message).foreach(_.length should not be 0)
    reports.foreach(report => withClue(report.message)(report.pass shouldBe false))
    reports.foreach(_.rdbInfo shouldBe None)
  }
}
