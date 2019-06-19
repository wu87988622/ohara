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

import com.island.ohara.common.rule.{LargeTest, MediumTest, SmallTest}
import com.island.ohara.it.IntegrationTest
import org.junit.Test
import org.scalatest.Matchers

class TestTestCases extends MediumTest with Matchers {

  /**
    * those classes don't have to extend correct super class.
    */
  private[this] val validTestGroups: Seq[Class[_]] = Seq(
    classOf[SmallTest],
    classOf[MediumTest],
    classOf[LargeTest],
    classOf[IntegrationTest]
  )

  /**
    * those classes don't have correct name format.
    */
  private[this] val ignoredTestClasses: Seq[Class[_]] = Seq(
    classOf[ListTestCases],
    classOf[com.island.ohara.it.agent.BasicTests4ClusterCollie],
    classOf[com.island.ohara.it.agent.BasicTests4ClusterCollieByConfigurator],
    classOf[com.island.ohara.it.agent.BasicTests4Collie],
    classOf[com.island.ohara.it.agent.BasicTests4StreamApp]
  )

  @Test
  def failIfTestHasNoTestCase(): Unit = {
    val classAndCases: Map[Class[_], Set[String]] = testClasses().map(clz => clz -> testCases(clz)).toMap
    classAndCases.size should not be 0
    val invalidTests: Map[Class[_], Set[String]] = classAndCases
    // legal test class should have a super class from validTestNames
      .filter(_._2.isEmpty)

    if (invalidTests.nonEmpty)
      throw new IllegalArgumentException(s"${invalidTests.keys.map(_.getName).mkString(",")} have no test cases")
  }

  /**
    * fail if any test case have not extended the test catalog.
    * We will find any possible superClass is in [SmallTest, MediumTest, LargeTest] or not.
    */
  @Test
  def testSuperClassOfTestClass(): Unit = {
    val classAndSuperClasses = testClasses().map(c => c -> superClasses(c))
    classAndSuperClasses.size should not be 0
    val invalidClasses = classAndSuperClasses.filterNot {
      case (_, superClasses) => superClasses.exists(validTestGroups.contains)
    }
    if (invalidClasses.nonEmpty)
      throw new IllegalArgumentException(
        s"${invalidClasses.mkString(",")}" +
          s"don't inherit test interfaces:${validTestGroups.mkString(",")}")
  }

  /**
    * the name of class extending test group should start with "Test".
    */
  @Test
  def testNameOfTestClasses(): Unit = {
    val invalidClasses = classesInTestScope()
      .filterNot(isAnonymous)
      .filterNot(ignoredTestClasses.contains)
      .map(clz => clz -> superClasses(clz))
      .filter {
        case (_, supers) => supers.exists(validTestGroups.contains)
      }
      .filterNot {
        case (clz, _) => clz.getSimpleName.startsWith("Test")
      }
      .map(_._1)
    if (invalidClasses.nonEmpty)
      throw new IllegalArgumentException(
        s"those classes:${invalidClasses.map(_.getName).mkString(",")} extend one of ${validTestGroups.mkString(",")} but their name don't start with Test")
  }
}
