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
  * this test class is used to find out the invalid format of builder. Builder pattern is across whole ohara project, and we try to make all impls
  * have similar form of method signature.
  */
class TestBuilderForm extends MediumTest with Matchers {

  private[this] val excludedClasses: Seq[Class[_]] = Seq(
    classOf[com.island.ohara.common.Builder[_]],
    classOf[com.island.ohara.streams.ostream.OStreamBuilder[_, _]]
  )

  @Test
  def test(): Unit = {
    val rootBuilder = classOf[com.island.ohara.common.Builder[_]].getName
    val pClasses = classesInProductionScope()
    pClasses.size should not be 0
    val invalidBuilders = pClasses
      .filter(_.getName.toLowerCase.endsWith("builder"))
      .filter(clz => !superClasses(clz).exists(_.getName == rootBuilder))
      .filter(clz => !excludedClasses.contains(clz))
    if (invalidBuilders.nonEmpty)
      throw new IllegalArgumentException(
        s"those builds:${invalidBuilders.map(_.getName).mkString(",")} do not extend $rootBuilder")
  }
}
