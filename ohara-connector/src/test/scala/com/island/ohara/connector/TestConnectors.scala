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

package com.island.ohara.connector

import java.lang.reflect.Modifier

import com.island.ohara.client.configurator.v0.Definition
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.kafka.connector.{RowSinkConnector, RowSourceConnector}
import org.junit.Test
import org.reflections.Reflections

import scala.collection.JavaConverters._

class TestConnectors extends OharaTest {
  @Test
  def testOfficialConnectorVersionCannotBeUnknown(): Unit = {
    localConnectorDefinitions
    // by convention, the official connectors are placed under package com.island.ohara.connector
      .filter(definition => definition.className.startsWith("com.island.ohara.connector"))
      .foreach { definition =>
        val connName = definition.className
        val settingDefs = definition.definitions
        settingDefs.foreach { settingDef =>
          val key = settingDef.key()
          if (key == "version" || key == "revision" || key == "author")
            if (settingDef.defaultValue() == "unknown")
              throw new AssertionError(s"The $connName $key cannot be unknown.")
        }
      }
  }

  /**
    * Dynamically instantiate local connector classes and then fetch the definitions from them.
    * @return local connector definitions
    */
  private[connector] lazy val localConnectorDefinitions: Seq[Definition] = {
    val reflections = new Reflections()
    val classes = reflections.getSubTypesOf(classOf[RowSourceConnector]).asScala ++ reflections
      .getSubTypesOf(classOf[RowSinkConnector])
      .asScala
    classes
    // the abstract class is not instantiable.
      .filterNot(clz => Modifier.isAbstract(clz.getModifiers))
      .flatMap { clz =>
        try Some((clz.getName, clz.newInstance().definitions().asScala))
        catch {
          case e: Throwable =>
            throw new AssertionError(s"failed to instantiate ${clz.getName} for Row[Source|Sink]Connector", e)
        }
      }
      .map { entry =>
        Definition(
          className = entry._1,
          definitions = entry._2
        )
      }
      .toSeq
  }
}
