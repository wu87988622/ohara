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

package com.island.ohara

import java.io.FileInputStream
import java.util.jar.JarInputStream
import java.util.regex.Pattern

import com.island.ohara.common.rule.{LargeTest, MediumTest, SmallTest}
import com.island.ohara.it.IntegrationTest
import org.junit.Test
import org.scalatest.Matchers

class TestTestCases extends MediumTest with Matchers {
  private[this] val validTestNames: Seq[String] = Seq(
    classOf[SmallTest],
    classOf[MediumTest],
    classOf[LargeTest],
    classOf[IntegrationTest]
  ).map(_.getName)

  private[this] def allSuperClasses(clz: Class[_]): Set[String] = new Iterator[Class[_]] {
    private[this] var current = clz.getSuperclass
    def hasNext: Boolean = current != null
    def next(): Class[_] = try current
    finally current = current.getSuperclass
  }.map(_.getName).toSet

  private[this] def testCases(clz: Class[_]): Set[String] = clz.getMethods
    .filter { m =>
      val annotations = m.getAnnotations
      if (annotations == null || annotations.isEmpty) false
      else annotations.exists(_.annotationType() == classOf[Test])
    }
    .map(_.getName)
    .toSet

  private[this] def testClasses(): Seq[Class[_]] = {
    val classLoader = ClassLoader.getSystemClassLoader
    val path = getClass.getPackage.getName.replace('.', '/') + "/"
    val pattern = Pattern.compile("^file:(.+\\.jar)!/" + path + "$")
    val urls = classLoader.getResources(path)
    Iterator
      .continually(urls.nextElement())
      .takeWhile(_ => urls.hasMoreElements)
      .map(url => pattern.matcher(url.getFile))
      .filter(_.find())
      .map(_.group(1))
      // hard code but it is ok since it is convention to name the tests jar as "tests.jar"
      .filter(_.contains("tests.jar"))
      .flatMap { f =>
        val jarInput = new JarInputStream(new FileInputStream(f))
        try Iterator
          .continually(jarInput.getNextJarEntry)
          .takeWhile(_ != null)
          .map(_.getName)
          .toArray
          .filter(_.endsWith(".class"))
          // scala may generate some extra classes
          .filterNot(_.contains('$'))
          .map(_.replace('/', '.'))
          .map(className => className.substring(0, className.length - ".class".length))
          .map(Class.forName)
          .filter(_.getSimpleName.startsWith("Test"))
        finally jarInput.close()
      }
      .toSeq
  }

  @Test
  def countTestCases(): Unit = println(s"count of test cases:${testClasses().map(testCases).map(_.size).sum}")

  @Test
  def displayAllCases(): Unit = testClasses().map(clz => clz.getName -> testCases(clz)).toMap.foreach {
    case (className, cases) =>
      cases.foreach { c =>
        println(s"ohara test case:$className.$c")
      }
  }

  @Test
  def failIfTestHasNoTestCase(): Unit = {
    val classAndCases: Map[Class[_], Set[String]] = testClasses().map(clz => clz -> testCases(clz)).toMap
    classAndCases.size should not be 0
    val invalidTests: Map[Class[_], Set[String]] = classAndCases.filter(_._2.isEmpty)

    if (invalidTests.nonEmpty)
      throw new IllegalArgumentException(s"${invalidTests.keys.map(_.getName).mkString(",")} have no test cases")
  }

  /**
    * fail if any test case have not extended the test catalog.
    * We will find any possible superClass is in [SmallTest, MediumTest, LargeTest] or not.
    */
  @Test
  def testSupperClassOfTestCases(): Unit = {
    val classAndSuperClasses = testClasses().map(c => c -> allSuperClasses(c))
    classAndSuperClasses.size should not be 0
    val invalidClasses = classAndSuperClasses.filterNot {
      case (_, superClasses) =>
        superClasses.exists(n => validTestNames.contains(n))
    }

    if (invalidClasses.nonEmpty)
      throw new IllegalArgumentException(
        s"${invalidClasses.mkString(",")}" +
          s"don't inherit test interfaces:${validTestNames.mkString(",")}")
  }
}
