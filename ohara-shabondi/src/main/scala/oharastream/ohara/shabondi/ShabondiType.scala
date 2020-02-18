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

package oharastream.ohara.shabondi

import scala.reflect.ClassTag

import oharastream.ohara.shabondi.sink.{Boot => SinkBoot}
import oharastream.ohara.shabondi.source.{Boot => SourceBoot}

/**
  * A helper class to reflect all "object" values in the subclass.
  * To get more information visit the following link :
  * https://medium.com/@yuriigorbylov/scala-enumerations-hell-5bdba2c1216
  *
  * @tparam Enu the class type
  */
abstract private[shabondi] class Enum[Enu: ClassTag] { self =>
  // save enum values
  val all: Seq[Enu] = {
    import scala.reflect.runtime.universe._
    val mirror      = runtimeMirror(self.getClass.getClassLoader)
    val classSymbol = mirror.classSymbol(self.getClass)

    classSymbol.toType.members
      .filter(_.isModule)
      .map(symbol => mirror.reflectModule(symbol.asModule).instance)
      .collect { case v: Enu => v }
      .toSeq
      .sortBy(_.toString)
  }

  /**
    * get the enum type according to input string.
    * Noted this seeks does not care for case.
    * @param name string name
    * @return enum type
    */
  def forName(name: String): Enu =
    all
      .find(_.toString.toLowerCase == name.toLowerCase)
      .getOrElse(throw new NoSuchElementException(s"Unexpected value : $name"))
}

sealed abstract class ShabondiType(val name: String, val className: String)

object ShabondiType extends Enum[ShabondiType] {
  case object Source extends ShabondiType("source", SourceBoot.getClass.getName.replaceAll("\\$", ""))
  case object Sink   extends ShabondiType("sink", SinkBoot.getClass.getName.replaceAll("\\$", ""))

  def apply(className: String): ShabondiType = {
    className match {
      case Source.className => Source
      case Sink.className   => Sink
      case _                => throw new IllegalArgumentException("Unknown shabondi service type")
    }
  }
}
