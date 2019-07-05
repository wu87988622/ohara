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

import com.island.ohara.common.pattern.{Builder, Creator}
import com.island.ohara.common.rule.MediumTest
import org.junit.Test
import org.scalatest.Matchers

/**
  * this test class is used to find out the invalid format of builder/creator.
  * Builder/Creator pattern is across whole ohara project, and we try to make all impls
  * have similar form of method signature.
  */
class TestCodePattern extends MediumTest with Matchers {

  @Test
  def testBuilder(): Unit =
    check(
      baseClass = classOf[Builder[_]],
      postfix = "builder",
      excludedClasses = Seq(
        classOf[Builder[_]],
        classOf[com.island.ohara.streams.ostream.OStreamBuilder[_, _]]
      )
    )

  @Test
  def testCreator(): Unit =
    check(
      baseClass = classOf[Creator[_]],
      postfix = "creator",
      excludedClasses = Seq(
        classOf[Creator[_]],
        // this may be deleted by #1713
        classOf[com.island.ohara.kafka.exception.ExceptionHandler.ExceptionHandlerCreator]
      )
    )

  private[this] def check(baseClass: Class[_], postfix: String, excludedClasses: Seq[Class[_]]): Unit = {
    val classes = classesInProductionScope()
    classes.size should not be 0
    val invalidClasses = classes
      .filterNot(clz => superClasses(clz).contains(baseClass))
      .filter(_.getName.toLowerCase.endsWith(postfix))
      .filter(clz => !excludedClasses.contains(clz))
    if (invalidClasses.nonEmpty)
      throw new IllegalArgumentException(
        s"those builds:${invalidClasses.map(_.getName).mkString(",")} do not extend $baseClass")
  }
}
