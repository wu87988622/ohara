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
  * The order of applying rules is shown below.
  * 1) convert the value to another type
  * 2) convert the null to the value of another key
  * 3) convert the null to default value
  * 4) value check
  * @tparam T scala object type
  */
trait JsonRefiner[T] {
  def format(format: RootJsonFormat[T]): JsonRefiner[T]

  //-------------------------[null to default]-------------------------//

  /**
    * convert the null value to random string
    * @param key  key
    * @return this refiner
    */
  def nullToRandomString(key: String): JsonRefiner[T] = nullToString(key, CommonUtils.randomString(10))

  def nullToString(key: String, defaultValue: String): JsonRefiner[T] = nullToJsValue(key, JsString(defaultValue))

  def nullToEmptyArray(key: String): JsonRefiner[T] = nullToJsValue(key, JsArray.empty)

  /**
    * convert the null value to random port and check the existent port.
    * @param key keys
    * @return this refiner
    */
  def nullToRandomPort(key: String): JsonRefiner[T] = nullToJsValue(key, JsNumber(CommonUtils.availablePort()))

  def nullToShort(key: String, value: Short): JsonRefiner[T] = nullToJsValue(key, JsNumber(value))

  def nullToInt(key: String, value: Int): JsonRefiner[T] = nullToJsValue(key, JsNumber(value))

  def nullToLong(key: String, value: Long): JsonRefiner[T] = nullToJsValue(key, JsNumber(value))

  def nullToDouble(key: String, value: Double): JsonRefiner[T] = nullToJsValue(key, JsNumber(value))

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
    *
    * This method is useful in working with deprecated key. For example, you plant to change the key from "k0" to "k1".
    * To avoid breaking the APIs, you can refine the parser with this method - nullToAnotherValueOfKey("k1", "k0") -
    * the json parser will seek the "k1" first and then find the "k0" if "k1" does not exist.
    *
    * Noted: it does nothing if both key and another key are nonexistent
    *
    * @param key the fist key mapped to input json that it can be ignored or null.
    *  @param anotherKey the second key mapped to input json that it must exists!!!
    * @return this refiner
    */
  def nullToAnotherValueOfKey(key: String, anotherKey: String): JsonRefiner[T]

  /**
    * set the default number for input keys. The default value will be added into json request if the associated key is nonexistent.
    * @param key key
    * @param defaultValue default value
    * @return this refiner
    */
  protected def nullToJsValue(key: String, defaultValue: JsValue): JsonRefiner[T]

  //-------------------------[more checks]-------------------------//

  /**
    * check whether target port is legal to connect. The legal range is (0, 65535].
    * @param key key
    * @return this refiner
    */
  def requireConnectionPort(key: String): JsonRefiner[T] = valueChecker(
    key, {
      case s: JsNumber if s.value.toInt > 0 && s.value <= 65535 => // pass
      case s: JsNumber =>
        throw DeserializationException(
          s"the connection port must be bigger than zero and small than 65536, but actual port is ${s.value}")
      case _ => // we don't care for other types
    }
  )

  /**
    * check whether target port is legal to bind. The legal range is (1024, 65535]
    * @param key key
    * @return this refiner
    */
  def requireBindPort(key: String): JsonRefiner[T] = valueChecker(
    key, {
      case s: JsNumber if s.value.toInt > 1024 && s.value <= 65535 => // pass
      case s: JsNumber =>
        throw DeserializationException(s"the connection port must be [1024, 65535), but actual port is ${s.value}")
      case _ => // we don't care for other types
    }
  )

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

  /**
    * throw exception if the input json has empty array.
    * Noted: this rule is applied to all key-value even if the pair is in nested object.
    * @return this refiner
    */
  def rejectEmptyArray(): JsonRefiner[T]

  /**
    * throw exception if the specific key of input json is associated to empty array.
    * @param key key
    * @return this refiner
    */
  def rejectEmptyArray(key: String): JsonRefiner[T] = valueChecker(
    key, {
      case s: JsArray if s.elements.isEmpty =>
        throw DeserializationException(s"the value array of $key can't be empty!!!")
      case _ => // we don't care for other types
    }
  )

  protected def valueChecker(key: String, checker: JsValue => Unit): JsonRefiner[T]

  //-------------------------[more conversion]-------------------------//

  /**
    * Set the auto-conversion to key that the string value wiil be converted to number. Hence, the key is able to accept
    * both string type and number type. By contrast, akka json will produce DeserializationException in parsing string to number.
    * @param key key
    * @return this refiner
    */
  def acceptStringToNumber(key: String): JsonRefiner[T] = valueConverter(
    key, {
      case s: JsString =>
        try JsNumber(s.value)
        catch {
          case e: NumberFormatException =>
            throw DeserializationException(s"the input string:${s.value} can't be converted to number", e)
        }
      case s: JsValue => s
    }
  )

  protected def valueConverter(key: String, converter: JsValue => JsValue): JsonRefiner[T]

  def refine: RootJsonFormat[T]
}

object JsonRefiner {
  def apply[T]: JsonRefiner[T] = new JsonRefiner[T] {
    private[this] var format: RootJsonFormat[T] = _
    private[this] var valueConverter: Map[String, JsValue => JsValue] = Map.empty
    private[this] var valueChecker: Map[String, JsValue => Unit] = Map.empty
    private[this] var nullToJsValue: Map[String, JsValue] = Map.empty
    private[this] var nullToAnotherValueOfKey: Map[String, String] = Map.empty
    private[this] var _rejectEmptyString: Boolean = false
    private[this] var _rejectNegativeNumber: Boolean = false
    private[this] var _rejectEmptyArray: Boolean = false

    override def format(format: RootJsonFormat[T]): JsonRefiner[T] = {
      this.format = Objects.requireNonNull(format)
      this
    }

    override protected def valueChecker(key: String, checker: JsValue => Unit): JsonRefiner[T] = {
      if (valueChecker.contains(CommonUtils.requireNonEmpty(key)))
        throw new IllegalArgumentException(s"the $key already has checker")
      this.valueChecker = this.valueChecker ++ Map(key -> Objects.requireNonNull(checker))
      this
    }

    override protected def valueConverter(key: String, converter: JsValue => JsValue): JsonRefiner[T] = {
      if (valueConverter.contains(CommonUtils.requireNonEmpty(key)))
        throw new IllegalArgumentException(s"the $key already has converter")
      this.valueConverter = this.valueConverter ++ Map(key -> Objects.requireNonNull(converter))
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

    override def rejectEmptyArray(): JsonRefiner[T] = {
      this._rejectEmptyArray = true
      this
    }

    override def nullToAnotherValueOfKey(key: String, anotherKey: String): JsonRefiner[T] = {
      if (nullToAnotherValueOfKey.contains(CommonUtils.requireNonEmpty(key)))
        throw new IllegalArgumentException(s"the $key have been associated to another key:$anotherKey")
      this.nullToAnotherValueOfKey = this.nullToAnotherValueOfKey ++ Map(key -> CommonUtils.requireNonEmpty(anotherKey))
      this
    }

    override protected def nullToJsValue(key: String, defaultValue: JsValue): JsonRefiner[T] = {
      if (nullToJsValue.contains(CommonUtils.requireNonEmpty(key)))
        throw new IllegalArgumentException(s"the $key have been associated to default value:$defaultValue")
      this.nullToJsValue = this.nullToJsValue ++ Map(key -> Objects.requireNonNull(defaultValue))
      this
    }

    override def refine: RootJsonFormat[T] = {
      Objects.requireNonNull(format)
      nullToJsValue.keys.foreach(CommonUtils.requireNonEmpty)
      valueConverter.keys.foreach(CommonUtils.requireNonEmpty)
      nullToAnotherValueOfKey.keys.foreach(CommonUtils.requireNonEmpty)
      nullToAnotherValueOfKey.values.foreach(CommonUtils.requireNonEmpty)
      new RootJsonFormat[T] {
        override def read(json: JsValue): T = {
          var fields = json.asJsObject.fields.filter {
            case (_, value) =>
              value match {
                case JsNull => false
                case _      => true
              }
          }

          // 1) convert the value to another type
          fields = fields ++ valueConverter
            .filter {
              case (key, _) => fields.contains(key)
            }
            .map {
              case (key, converter) => key -> converter(fields(key))
            }
            .filter {
              case (_, jsValue) =>
                jsValue match {
                  case JsNull => false
                  case _      => true
                }
            }

          // 2) convert the null to the value of another key
          fields = fields ++ nullToAnotherValueOfKey
            .filterNot(pair => fields.contains(pair._1))
            .filter(pair => fields.contains(pair._2))
            .map {
              case (key, anotherKye) => key -> fields(anotherKye)
            }

          // 3) convert the null to default value
          fields = fields ++ nullToJsValue.map {
            case (key, defaultValue) =>
              key -> fields.getOrElse(key, defaultValue)
          }

          // 4) check empty string
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

          // 5) check negative number
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

          // 6) check empty array
          def checkEmptyArray(key: String, s: JsArray): Unit = if (s.elements.isEmpty)
            throw DeserializationException(s"the value of $key MUST be NOT empty array!!!")
          def checkJsValueForEmptyArray(k: String, v: JsValue): Unit = v match {
            case s: JsArray => checkEmptyArray(k, s)
            case s: JsObject =>
              s.fields.foreach {
                case (key, value) => checkJsValueForEmptyArray(key, value)
              }
            case _ => // nothing
          }
          if (_rejectEmptyArray) fields.foreach {
            case (key, value) => checkJsValueForEmptyArray(key, value)
          }

          // 7) custom check
          valueChecker.foreach {
            case (key, checker) => checker(fields.getOrElse(key, JsNull))
          }

          format.read(JsObject(fields))
        }
        override def write(obj: T): JsValue = format.write(obj)
      }
    }
  }
}
