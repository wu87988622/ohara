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

package com.island.ohara.assembly

import com.island.ohara.common.rule.MediumTest
import org.junit.Test
import org.scalatest.Matchers

/**
  * this is a QA-purposed test class that it offers two methods to show the required information for QA. It consists of
  * 1) count of test cases (see countTestCases)
  * 2) list full name of all test class (see displayAllCases_
  * Normally, this class should not fail.
  */
class ListTestCases extends MediumTest with Matchers {

  /**
    * DON'T change the output message since our QA will parse the output log to find the count of test cases.
    */
  @Test
  def countTestCases(): Unit = println(s"count of test cases:${testClasses().map(testCases).map(_.size).sum}")

  /**
    * DON'T change the output message since our QA will parse the output log to find the test cases.
    */
  @Test
  def displayAllCases(): Unit = testClasses().map(clz => clz.getName -> testCases(clz)).toMap.foreach {
    case (className, cases) =>
      cases.foreach { c =>
        println(s"ohara test case:$className.$c")
      }
  }
}
