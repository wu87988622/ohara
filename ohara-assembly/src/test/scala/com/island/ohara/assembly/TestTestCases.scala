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

import java.lang.reflect.Method

import com.island.ohara.common.rule.OharaTest
import com.island.ohara.it.IntegrationTest
import com.island.ohara.shabondi.TestWebServer
import org.junit.Test
import org.scalatest.Matchers

class TestTestCases extends OharaTest with Matchers {

  /**
    * those classes don't have to extend correct super class.
    */
  private[this] val validTestGroups: Seq[Class[_]] = Seq(
    classOf[OharaTest],
    classOf[IntegrationTest]
  )

  /**
    * those classes don't have correct name format.
    */
  private[this] val ignoredTestClasses: Seq[Class[_]] = Seq(
    classOf[ListTestCases]
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
      // there are many basic test cases for the various cases. Their names don't start with "Test" since they
      // are no prepared to test directly.
      .filterNot(isAbstract)
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
        s"those classes:${invalidClasses.map(_.getName).mkString(",")} extend one of ${validTestGroups.mkString(",")} but they are not abstract class, " +
          "and their name don't start with \"Test\"")
  }

  @Test
  def testCaseShouldHaveAnnotation(): Unit = {
    val ignoredClasses: Set[Class[_]] = Set(classOf[TestWebServer])
    val illegalCases: Map[Class[_], Set[Method]] = testClasses()
      .filter(clz => clz.getMethods != null)
      .filterNot(ignoredClasses.contains)
      .map(
        clz =>
          clz -> clz.getMethods
            .filter(_.getName.toLowerCase.startsWith("test"))
            .filter { m =>
              val annotations = m.getAnnotations
              if (annotations == null || annotations.isEmpty) true
              else !annotations.exists(_.annotationType() == classOf[Test])
            }
            .toSet)
      .filter(_._2.nonEmpty)
      .toMap
    if (illegalCases.nonEmpty) {
      val sum = illegalCases
        .map {
          case (clz, methods) => methods.map(m => s"${clz.getName}.${m.getName}").mkString(",")
        }
        .mkString("|")
      throw new AssertionError(s"we neglected to add @Test to following test cases $sum")
    }
  }
}
