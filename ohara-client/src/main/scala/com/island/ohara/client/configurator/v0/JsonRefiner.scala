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
    * reject the request having a key which is associated to empty string.
    * Noted: this rule is applied to all key-value even if the pair is in nested object.
    * @return this refiner
    */
  def rejectEmptyString(): JsonRefiner[T]

  /**
    * reject the value having negative number. For instance, the following request will be rejected.
    * {
    *   "a": -1
    * }
    * Noted: this rule is applied to all key-value even if the pair is in nested object.
    * @return this refiner
    */
  def rejectNegativeNumber(): JsonRefiner[T]

  def defaultShort(key: String, value: Short): JsonRefiner[T] = defaultShorts(Map(key -> value))

  def defaultShorts(keysAndDefaults: Map[String, Short]): JsonRefiner[T] =
    defaultNumbers(keysAndDefaults.map {
      case (key, value) => key -> JsNumber(value)
    })

  def defaultInt(key: String, value: Int): JsonRefiner[T] = defaultInts(Map(key -> value))

  def defaultInts(keysAndDefaults: Map[String, Int]): JsonRefiner[T] =
    defaultNumbers(keysAndDefaults.map {
      case (key, value) => key -> JsNumber(value)
    })

  def defaultLong(key: String, value: Long): JsonRefiner[T] = defaultLongs(Map(key -> value))

  def defaultLongs(keysAndDefaults: Map[String, Long]): JsonRefiner[T] =
    defaultNumbers(keysAndDefaults.map {
      case (key, value) => key -> JsNumber(value)
    })

  def defaultDouble(key: String, value: Double): JsonRefiner[T] = defaultDoubles(Map(key -> value))

  def defaultDoubles(keysAndDefaults: Map[String, Double]): JsonRefiner[T] =
    defaultNumbers(keysAndDefaults.map {
      case (key, value) =>
        key -> (JsNumber(value) match {
          case s: JsNumber => s
          case _           => throw new IllegalArgumentException(s"illegal double value:$value")
        })
    })

  /**
    * set the default number for input keys. The default value will be added into json request if the associated key is nonexistent.
    * @param keyAndDefaultNumber keys and their default value
    * @return this refiner
    */
  protected def defaultNumbers(keyAndDefaultNumber: Map[String, JsNumber]): JsonRefiner[T]

  def defaultToAnother(key: String, anotherKey: String): JsonRefiner[T] = defaultToAnother(
    Map(CommonUtils.requireNonEmpty(key) -> CommonUtils.requireNonEmpty(anotherKey)))

  /**
    * auto-fill the value of key by another key's value. For example
    * defaultToAnother(Map("k0", "k1"))
    * input:
    * {
    *   "k1": "abc"
    * }
    * output:
    * {
    *   "k0": "abc",
    *   "k1": "abc"
    * }
    * Noted: another key must be exist. Otherwise, DeserializationException will be thrown.
    *
    * @param keyPairs the fist key mapped to input json that it can be ignored or null. the second key mapped to input json that it must exists!!!
    * @return this refiner
    */
  def defaultToAnother(keyPairs: Map[String, String]): JsonRefiner[T]

  def refine: RootJsonFormat[T]
}

object JsonRefiner {
  def apply[T]: JsonRefiner[T] = new JsonRefiner[T] {
    private[this] var format: RootJsonFormat[T] = _
    private[this] var nullToEmptyArray: Seq[String] = Seq.empty
    private[this] var connectionPort: Seq[String] = Seq.empty
    private[this] var nullToRandomBindPort: Seq[String] = Seq.empty
    private[this] var nullToRandomString: Seq[String] = Seq.empty
    private[this] var defaultNumbers: Map[String, JsNumber] = Map.empty
    private[this] var defaultToAnother: Map[String, String] = Map.empty
    private[this] var _rejectEmptyString: Boolean = false
    private[this] var _rejectNegativeNumber: Boolean = false

    override def format(format: RootJsonFormat[T]): JsonRefiner[T] = {
      this.format = Objects.requireNonNull(format)
      this
    }

    override def nullToEmptyArray(keys: Seq[String]): JsonRefiner[T] = {
      this.nullToEmptyArray = nullToEmptyArray ++ Objects.requireNonNull(keys).map(CommonUtils.requireNonEmpty)
      this
    }

    override def connectionPort(keys: Seq[String]): JsonRefiner[T] = {
      this.connectionPort = connectionPort ++ Objects.requireNonNull(keys).map(CommonUtils.requireNonEmpty)
      this
    }

    override def nullToRandomPort(keys: Seq[String]): JsonRefiner[T] = {
      this.nullToRandomBindPort = nullToRandomBindPort ++ Objects.requireNonNull(keys).map(CommonUtils.requireNonEmpty)
      this
    }

    override def nullToRandomString(keys: Seq[String]): JsonRefiner[T] = {
      this.nullToRandomString = nullToRandomString ++ Objects.requireNonNull(keys).map(CommonUtils.requireNonEmpty)
      this
    }

    override def rejectEmptyString(): JsonRefiner[T] = {
      this._rejectEmptyString = true
      this
    }

    override def rejectNegativeNumber(): JsonRefiner[T] = {
      this._rejectNegativeNumber = true
      this
    }

    override protected def defaultNumbers(defaultNumbers: Map[String, JsNumber]): JsonRefiner[T] = {
      this.defaultNumbers = this.defaultNumbers ++ defaultNumbers
      this
    }

    override def defaultToAnother(keyPairs: Map[String, String]): JsonRefiner[T] = {
      this.defaultToAnother = this.defaultToAnother ++ keyPairs
      this
    }

    override def refine: RootJsonFormat[T] = {
      Objects.requireNonNull(format)
      // check the duplicate keys in different groups
      // those rules are used to auto-fill a value to nonexistent key. Hence, we disallow to set multiple rules on the same key.
      if (nullToEmptyArray.size + connectionPort.size + nullToRandomBindPort.size + nullToRandomString.size + defaultNumbers.size + defaultToAnother.size
            != (nullToEmptyArray ++ connectionPort ++ nullToRandomBindPort ++ nullToRandomString ++ defaultNumbers.keys ++ defaultToAnother.keys).toSet.size)
        throw new IllegalArgumentException(
          s"duplicate key in different groups is illegal."
            + s", nullToEmptyArray:${nullToEmptyArray.mkString(",")}"
            + s", connectionPort:${connectionPort.mkString(",")}"
            + s", nullToRandomPort:${nullToRandomBindPort.mkString(",")}"
            + s", nullToRandomString:${nullToRandomString.mkString(",")}"
            + s", keyAndDefaultNumber.keys:${defaultNumbers.keys.mkString(",")}"
            + s", defaultToAnother.keys:${defaultToAnother.keys.mkString(",")}"
        )
      nullToEmptyArray.foreach(CommonUtils.requireNonEmpty)
      connectionPort.foreach(CommonUtils.requireNonEmpty)
      nullToRandomBindPort.foreach(CommonUtils.requireNonEmpty)
      nullToRandomString.foreach(CommonUtils.requireNonEmpty)
      defaultNumbers.keys.foreach(CommonUtils.requireNonEmpty)
      defaultToAnother.keys.foreach(CommonUtils.requireNonEmpty)
      defaultToAnother.values.foreach(CommonUtils.requireNonEmpty)

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

          // convert null to JsNumber
          fields = fields ++ defaultNumbers.map {
            case (key, value) =>
              key -> fields
                .get(key)
                .map {
                  case s: JsNumber => s
                  case _ =>
                    throw DeserializationException(
                      s"$key should be associated Number type, but actual type is ${fields(key)}")
                }
                .getOrElse(value)
          }

          // convert the null to the value of another key
          fields = fields ++ defaultToAnother.filterNot(pair => fields.contains(pair._1)).map {
            case (key, anotherKye) =>
              if (!fields.contains(anotherKye)) throw DeserializationException(s"$anotherKye is required!!!")
              key -> fields(anotherKye)
          }

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

          // check empty string
          def checkEmptyString(key: String, s: JsString): Unit =
            if (s.value.isEmpty) throw DeserializationException(s"the value of $key can't be empty string!!!")
          def checkJsValueForEmptyString(k: String, v: JsValue): Unit = v match {
            case s: JsString => checkEmptyString(k, s)
            case s: JsArray  => s.elements.foreach(v => checkJsValueForEmptyString(k, v))
            case s: JsObject =>
              s.fields.foreach {
                case (key, value) => checkJsValueForEmptyString(key, value)
              }
            case _ => // nothing
          }
          if (_rejectEmptyString) fields.foreach {
            case (key, value) => checkJsValueForEmptyString(key, value)
          }

          // check negative number
          def checkNegativeNumber(key: String, s: JsNumber): Unit = if (s.value < 0)
            throw DeserializationException(s"the value of $key MUST be bigger than or equal to zero!!!")
          def checkJsValueForNegativeNumber(k: String, v: JsValue): Unit = v match {
            case s: JsNumber => checkNegativeNumber(k, s)
            case s: JsArray  => s.elements.foreach(v => checkJsValueForNegativeNumber(k, v))
            case s: JsObject =>
              s.fields.foreach {
                case (key, value) => checkJsValueForNegativeNumber(key, value)
              }
            case _ => // nothing
          }
          if (_rejectNegativeNumber) fields.foreach {
            case (key, value) => checkJsValueForNegativeNumber(key, value)
          }

          format.read(JsObject(fields))
        }
        override def write(obj: T): JsValue = format.write(obj)
      }
    }
  }
}
