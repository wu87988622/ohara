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

  /**
    * check whether target port is legal to connect. The legal range is (0, 65535].
    * @param key key
    * @return this refiner
    */
  def requireConnectionPort(key: String): JsonRefiner[T] = keyChecker(
    key, {
      case s: JsNumber if s.value.toInt > 0 && s.value <= 65535 => s
      case s: JsNumber =>
        throw DeserializationException(
          s"the connection port must be bigger than zero and small than 65536, but actual port is ${s.value}")
      case value =>
        throw DeserializationException(s"$key should be associated number type, but actual type is $value")
    }
  )

  /**
    * check whether target port is legal to bind. The legal range is (1024, 65535]
    * @param key key
    * @return this refiner
    */
  def requireBindPort(key: String): JsonRefiner[T] = keyChecker(
    key, {
      case s: JsNumber if s.value.toInt > 1024 && s.value <= 65535 => s
      case s: JsNumber =>
        throw DeserializationException(s"the connection port must be [1024, 65535), but actual port is ${s.value}")
      case value =>
        throw DeserializationException(s"$key should be associated number type, but actual type is $value")
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
    * Noted: another key must be exist. Otherwise, DeserializationException will be thrown.
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

  protected def keyChecker(key: String, checker: JsValue => JsValue): JsonRefiner[T]

  def refine: RootJsonFormat[T]
}

object JsonRefiner {
  def apply[T]: JsonRefiner[T] = new JsonRefiner[T] {
    private[this] var format: RootJsonFormat[T] = _
    private[this] var keyChecker: Map[String, JsValue => JsValue] = Map.empty
    private[this] var nullToJsValue: Map[String, JsValue] = Map.empty
    private[this] var nullToAnotherValueOfKey: Map[String, String] = Map.empty
    private[this] var _rejectEmptyString: Boolean = false
    private[this] var _rejectNegativeNumber: Boolean = false
    private[this] var _rejectEmptyArray: Boolean = false

    override def format(format: RootJsonFormat[T]): JsonRefiner[T] = {
      this.format = Objects.requireNonNull(format)
      this
    }

    override protected def keyChecker(key: String, checker: JsValue => JsValue): JsonRefiner[T] = {
      if (keyChecker.contains(CommonUtils.requireNonEmpty(key)))
        throw new IllegalArgumentException(s"the $key already has checker")
      this.keyChecker = this.keyChecker ++ Map(key -> Objects.requireNonNull(checker))
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
      checkDuplicateRulesOnSameKey(CommonUtils.requireNonEmpty(key))
      this.nullToAnotherValueOfKey = this.nullToAnotherValueOfKey ++ Map(key -> CommonUtils.requireNonEmpty(anotherKey))
      this
    }

    override protected def nullToJsValue(key: String, defaultValues: JsValue): JsonRefiner[T] = {
      checkDuplicateRulesOnSameKey(CommonUtils.requireNonEmpty(key))
      this.nullToJsValue = this.nullToJsValue ++ Map(key -> Objects.requireNonNull(defaultValues))
      this
    }

    private[this] def checkDuplicateRulesOnSameKey(inComingKey: String): Unit =
      if ((nullToJsValue.keySet ++ nullToAnotherValueOfKey.keySet).contains(inComingKey))
        throw new IllegalArgumentException(s"$inComingKey already exists!!!")

    override def refine: RootJsonFormat[T] = {
      Objects.requireNonNull(format)
      // check the duplicate keys in different groups
      // those rules are used to auto-fill a value to nonexistent key. Hence, we disallow to set multiple rules on the same key.
      if (nullToJsValue.size + nullToAnotherValueOfKey.size != (nullToJsValue.keys ++ nullToAnotherValueOfKey.keys).toSet.size)
        throw new IllegalArgumentException(
          s"duplicate key in different groups is illegal."
            + s", nullToJsValue:${nullToJsValue.keys.mkString(",")}"
            + s", defaultToAnother.keys:${nullToAnotherValueOfKey.keys.mkString(",")}"
        )
      nullToJsValue.keys.foreach(CommonUtils.requireNonEmpty)
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
          // convert the null to default value
          fields = fields ++ nullToJsValue.map {
            case (key, defaultValue) =>
              key -> fields.getOrElse(key, defaultValue)
          }

          // convert the null to the value of another key
          fields = fields ++ nullToAnotherValueOfKey.filterNot(pair => fields.contains(pair._1)).map {
            case (key, anotherKye) =>
              if (!fields.contains(anotherKye)) throw DeserializationException(s"$anotherKye is required!!!")
              key -> fields(anotherKye)
          }

          // check the connection port
          keyChecker.foreach {
            case (key, checker) =>
              fields.get(key).foreach(checker)
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

          // check empty array
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

          format.read(JsObject(fields))
        }
        override def write(obj: T): JsValue = format.write(obj)
      }
    }
  }
}
