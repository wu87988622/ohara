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

import java.lang.reflect.Modifier

import com.island.ohara.client.configurator.v0.FileInfoApi.FileInfo
import com.island.ohara.client.configurator.v0.InspectApi.ClassInfo
import com.island.ohara.kafka.connector.json.ConnectorDefUtils
import com.island.ohara.kafka.connector.{RowSinkConnector, RowSourceConnector, WithDefinitions}
import com.island.ohara.streams.StreamApp
import com.typesafe.scalalogging.Logger
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.ConfigurationBuilder

import scala.collection.JavaConverters._
object ReflectionUtils {

  private[this] val LOG = Logger(ReflectionUtils.getClass)

  type Source = String
  type Sink = String
  type Stream = String

  /**
    * load the connectors class and streamApp classes from specific file
    * @param fileInfo file info
    * @return (sources, sinks, streamApps)
    */
  def loadConnectorAndStreamClasses(fileInfo: FileInfo): (Seq[Source], Seq[Sink], Seq[Stream]) = {

    /**
      * we don't define the class loader to this reflections since we don't care for the "class type"
      */
    val reflections = new Reflections(new ConfigurationBuilder().addUrls(fileInfo.url))

    def fetch(clz: Class[_]): Seq[String] =
      // classOf[SubTypesScanner].getSimpleName is hard-code since Reflections does not expose it ...
      reflections.getStore.getAll(classOf[SubTypesScanner].getSimpleName, clz.getName).asScala.toSeq

    (fetch(classOf[RowSourceConnector]), fetch(classOf[RowSinkConnector]), fetch(classOf[StreamApp]))
  }

  /**
    * Dynamically instantiate local connector classes and then fetch the definitions from them.
    * @return local connector definitions
    */
  lazy val localConnectorDefinitions: Seq[ClassInfo] =
    new Reflections()
      .getSubTypesOf(classOf[WithDefinitions])
      .asScala
      .toSeq
      // the abstract class is not instantiable.
      .filterNot(clz => Modifier.isAbstract(clz.getModifiers))
      .flatMap { clz =>
        try Some((clz.getName, clz.newInstance().definitions().asScala))
        catch {
          case e: Throwable =>
            LOG.error(s"failed to instantiate ${clz.getName} for RowSourceConnector", e)
            None
        }
      }
      .map {
        case (className, definitions) =>
          ClassInfo(
            className = className,
            classType = definitions
              .filter(_.hasDefault)
              .find(_.key() == ConnectorDefUtils.KIND_KEY)
              .map(_.defaultString)
              .getOrElse("connector"),
            settingDefinitions = definitions
          )
      }
}
