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

package com.island.ohara.client.configurator.v0

import java.util.Objects

import com.island.ohara.common.util.CommonUtils
import spray.json.{DeserializationException, JsArray, JsNull, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

/**
  * this is a akka-json representation++ which offers many useful sugar conversion of input json.
  * @tparam T scala object type
  */
trait JsonRefiner[T] {
  def format(format: RootJsonFormat[T]): JsonRefiner[T]

  def nullToRandomString(key: String): JsonRefiner[T] = nullToRandomString(Seq(key))

  /**
    * convert the null value to random string
    * @param keys keys
    * @return this refiner
    */
  def nullToRandomString(keys: Seq[String]): JsonRefiner[T]

  def nullToEmptyArray(key: String): JsonRefiner[T] = nullToEmptyArray(Seq(key))

  /**
    * convert the null value to empty array
    * @param keys keys
    * @return this refiner
    */
  def nullToEmptyArray(keys: Seq[String]): JsonRefiner[T]

  def nullToRandomBindPort(key: String): JsonRefiner[T] = nullToRandomPort(Seq(key))

  /**
    * convert the null value to random port and check the existent port.
    * @param keys keys
    * @return this refiner
    */
  def nullToRandomPort(keys: Seq[String]): JsonRefiner[T]

  def connectionPort(key: String): JsonRefiner[T] = connectionPort(Seq(key))

  /**
    * check whether target port is legal
    * @param keys keys
    * @return this refiner
    */
  def connectionPort(keys: Seq[String]): JsonRefiner[T]

  /**
    * reject thr request having a key which is associated to empty string
    * @return this refiner
    */
  def rejectEmptyString(): JsonRefiner[T]

  def refine: RootJsonFormat[T]
}

object JsonRefiner {
  def apply[T]: JsonRefiner[T] = new JsonRefiner[T] {
    private[this] var format: RootJsonFormat[T] = _
    private[this] var nullToEmptyArray: Seq[String] = Seq.empty
    private[this] var connectionPort: Seq[String] = Seq.empty
    private[this] var nullToRandomBindPort: Seq[String] = Seq.empty
    private[this] var nullToRandomString: Seq[String] = Seq.empty
    private[this] var _rejectEmptyString: Boolean = false

    override def format(format: RootJsonFormat[T]): JsonRefiner[T] = {
      this.format = Objects.requireNonNull(format)
      this
    }

    override def nullToEmptyArray(keys: Seq[String]): JsonRefiner[T] = {
      this.nullToEmptyArray = Objects.requireNonNull(keys).map(CommonUtils.requireNonEmpty)
      this
    }

    override def connectionPort(keys: Seq[String]): JsonRefiner[T] = {
      this.connectionPort = Objects.requireNonNull(keys).map(CommonUtils.requireNonEmpty)
      this
    }

    override def nullToRandomPort(keys: Seq[String]): JsonRefiner[T] = {
      this.nullToRandomBindPort = Objects.requireNonNull(keys).map(CommonUtils.requireNonEmpty)
      this
    }

    override def nullToRandomString(keys: Seq[String]): JsonRefiner[T] = {
      this.nullToRandomString = Objects.requireNonNull(keys).map(CommonUtils.requireNonEmpty)
      this
    }

    override def rejectEmptyString(): JsonRefiner[T] = {
      this._rejectEmptyString = true
      this
    }

    override def refine: RootJsonFormat[T] = {
      Objects.requireNonNull(format)
      // check the duplicate keys in different groups
      if (nullToEmptyArray.size + connectionPort.size + nullToRandomBindPort.size + nullToRandomString.size
            != (nullToEmptyArray ++ connectionPort ++ nullToRandomBindPort ++ nullToRandomString).toSet.size)
        throw new IllegalArgumentException(
          s"duplicate key in different groups is illegal."
            + s", nullToEmptyArray:${nullToEmptyArray.mkString(",")}"
            + s", connectionPort:${connectionPort.mkString(",")}"
            + s", nullToRandomPort:${nullToRandomBindPort.mkString(",")}"
            + s", nullToRandomString:${nullToRandomString.mkString(",")}")
      nullToEmptyArray.foreach(CommonUtils.requireNonEmpty)
      connectionPort.foreach(CommonUtils.requireNonEmpty)
      nullToRandomBindPort.foreach(CommonUtils.requireNonEmpty)
      nullToRandomString.foreach(CommonUtils.requireNonEmpty)

      new RootJsonFormat[T] {
        override def read(json: JsValue): T = {
          var fields = json.asJsObject.fields.filter {
            case (_, value) =>
              value match {
                case JsNull => false
                case _      => true
              }
          }
          // convert the null to empty array
          fields = fields ++ nullToEmptyArray.map { key =>
            key -> fields
              .get(key)
              .map {
                case s: JsArray => s
                case _ =>
                  throw DeserializationException(
                    s"$key should be associated array type, but actual type is ${fields(key)}")
              }
              .getOrElse(JsArray.empty)
          }.toMap

          // convert the null to random port
          fields = fields ++ nullToRandomBindPort.map { key =>
            key -> fields
              .get(key)
              .map {
                case s: JsNumber if s.value.toInt == 0 => JsNumber(CommonUtils.availablePort())
                case s: JsNumber if s.value.toInt < 0 =>
                  throw DeserializationException(
                    s"the value of $key must be bigger than or equal with zero, but actual value is ${s.value}")
                case s: JsNumber if s.value.toInt <= 1024 =>
                  throw DeserializationException(s"the port:${s.value} of $key is privileged port!!!")
                case s: JsNumber if s.value.toInt > 65535 =>
                  throw DeserializationException(s"the port:${s.value} of $key can't be bigger than 65535")
                case s: JsNumber if s.value.toInt > 0 => s
                case _ =>
                  throw DeserializationException(
                    s"$key should be associated Number type, but actual type is ${fields(key)}")
              }
              .getOrElse(JsNumber(CommonUtils.availablePort()))
          }.toMap

          // convert the null to random string
          fields = fields ++ nullToRandomString.map { key =>
            key -> fields
              .get(key)
              .map {
                case s: JsString => s
                case _ =>
                  throw DeserializationException(
                    s"$key should be associated String type, but actual type is ${fields(key)}")
              }
              .getOrElse(JsString(CommonUtils.randomString(10)))
          }.toMap

          // check the connection port
          connectionPort.foreach { key =>
            fields.get(key).foreach {
              case s: JsNumber if s.value.toInt <= 0 =>
                throw DeserializationException(
                  s"the value of $key must be bigger than zero, but actual value is ${s.value}")
              case s: JsNumber if s.value.toInt > 65535 =>
                throw DeserializationException(s"the port:${s.value} of $key can't be bigger than 65535")
              case s: JsNumber => s
              case _ =>
                throw DeserializationException(
                  s"$key should be associated number type, but actual type is ${fields(key)}")
            }
          }

          if (_rejectEmptyString) fields.foreach {
            case (key, value) =>
              value match {
                case s: JsString =>
                  if (s.value.isEmpty) throw DeserializationException(s"the value of $key can't be empty string!!!")
                case _ => // nothing
              }
          }

          format.read(JsObject(fields))
        }
        override def write(obj: T): JsValue = format.write(obj)
      }
    }

  }
}
