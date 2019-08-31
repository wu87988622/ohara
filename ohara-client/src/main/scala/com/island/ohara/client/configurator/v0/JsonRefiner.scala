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

import com.island.ohara.client.configurator.v0.JsonRefiner.{ArrayRestriction, StringRestriction}
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
  def nullToRandomString(key: String): JsonRefiner[T] = nullToJsValue(key, () => JsString(CommonUtils.randomString(10)))

  def nullToString(key: String, defaultValue: String): JsonRefiner[T] = nullToJsValue(key, () => JsString(defaultValue))

  def nullToString(key: String, defaultValue: () => String): JsonRefiner[T] =
    nullToJsValue(key, () => JsString(defaultValue()))

  def nullToEmptyArray(key: String): JsonRefiner[T] = nullToJsValue(key, () => JsArray.empty)

  def nullToEmptyObject(key: String): JsonRefiner[T] = nullToJsValue(key, () => JsObject.empty)

  /**
    * convert the null value to random port and check the existent port.
    * @param key keys
    * @return this refiner
    */
  def nullToRandomPort(key: String): JsonRefiner[T] = nullToJsValue(key, () => JsNumber(CommonUtils.availablePort()))

  def nullToShort(key: String, value: Short): JsonRefiner[T] = nullToJsValue(key, () => JsNumber(value))

  def nullToInt(key: String, value: Int): JsonRefiner[T] = nullToJsValue(key, () => JsNumber(value))

  def nullToLong(key: String, value: Long): JsonRefiner[T] = nullToJsValue(key, () => JsNumber(value))

  def nullToDouble(key: String, value: Double): JsonRefiner[T] = nullToJsValue(key, () => JsNumber(value))

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
  protected def nullToJsValue(key: String, defaultValue: () => JsValue): JsonRefiner[T]

  //-------------------------[more checks]-------------------------//

  /**
    * require the key for input json. It produces exception if input json is lack of key.
    * @param key required key
    * @return this refiner
    */
  def requireKey(key: String): JsonRefiner[T] = requireKeys(Set(key))

  /**
    * require the keys for input json. It produces exception if input json is lack of those keys.
    * @param keys required keys
    * @return this refiner
    */
  def requireKeys(keys: Set[String]): JsonRefiner[T] = keysChecker { inputKeys =>
    val nonexistentKeys = keys.diff(inputKeys)
    if (nonexistentKeys.nonEmpty)
      throw DeserializationException(
        s"${nonexistentKeys.mkString(",")} are required!!!",
        fieldNames = nonexistentKeys.toList
      )
  }

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
          s"""the connection port must be bigger than zero and small than 65536, but actual port is \"${s.value}\"""",
          fieldNames = List(key))
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
        throw DeserializationException(
          s"""the connection port must be [1024, 65535), but actual port is \"${s.value}\"""",
          fieldNames = List(key))
      case _ => // we don't care for other types
    }
  )
  def rejectKeyword(keyword: String): JsonRefiner[T] = valueChecker(
    keyword, {
      case JsNull => // nothing
      case _      => throw DeserializationException(s"""the \"$keyword\" is a illegal word!!!""")
    }
  )

  /**
    * reject the request having a key which is associated to empty string.
    * Noted: this rule is applied to all key-value even if the pair is in nested object.
    * @return this refiner
    */
  def rejectEmptyString(): JsonRefiner[T]

  /**
    * throw exception if the specific key of input json is associated to empty string.
    * This method check only the specific key. By contrast, rejectEmptyString() checks values for all keys.
    * @param key key
    * @return this refiner
    */
  def rejectEmptyString(key: String): JsonRefiner[T] = valueChecker(
    key, {
      case s: JsString if s.value.isEmpty =>
        throw DeserializationException(s"""the value of \"$key\" can't be empty string!!!""", fieldNames = List(key))
      case _ => // we don't care for other types
    }
  )

  /**
    * throw exception if the specific key of input json is associated to either negative number or zero.
    * @param key key
    * @return this refiner
    */
  def requirePositiveNumber(key: String): JsonRefiner[T] = valueChecker(
    key, {
      case s: JsNumber if s.value <= 0 =>
        throw DeserializationException(s"""the \"${s.value}\" of \"$key\" can't be either negative or zero!!!""",
                                       fieldNames = List(key))
      case _ => // we don't care for other types
    }
  )

  /**
    * throw exception if the specific key of input json is associated to negative number.
    * This method check only the specific key. By contrast, rejectNegativeNumber() checks values for all keys.
    * @param key key
    * @return this refiner
    */
  def rejectNegativeNumber(key: String): JsonRefiner[T] = valueChecker(
    key, {
      case s: JsNumber if s.value < 0 =>
        throw DeserializationException(s"""the \"${s.value}\" of \"$key\" can't be negative value!!!""",
                                       fieldNames = List(key))
      case _ => // we don't care for other types
    }
  )

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
  def rejectEmptyArray(key: String): JsonRefiner[T] = arrayRestriction(key).rejectEmpty().toRefiner

  /**
    * add the array restriction to specific value.
    * It throws exception if the specific key of input json violates any restriction.
    * Noted: the empty restriction make the checker to reject all input values.
    * @param key key
    * @return this refiner
    */
  def arrayRestriction(key: String): ArrayRestriction[T] = (checkers: Seq[(String, JsArray) => Unit]) =>
    valueChecker(
      key, {
        case arr: JsArray => checkers.foreach(_.apply(key, arr))
        case _ =>
          throw DeserializationException(
            s"""the value mapped to \"$key\" must be array type""",
            fieldNames = List(key)
          )
      }
  )

  def stringRestriction(key: String): StringRestriction[T] = stringRestriction(Set(key))

  /**
    * add the string restriction to specific value. It throws exception if the input value can't pass any restriction.
    * Noted: the empty restriction make the checker to reject all input values. However, you are disable to create a
    * restriction instance without any restriction rules. see our implementation.
    * @param keys keys
    * @return refiner
    */
  def stringRestriction(keys: Set[String]): StringRestriction[T] =
    (legalPairs: Seq[(Char, Char)], lengthLimit: Int) => {
      keys.foreach { key =>
        valueChecker(
          key, {
            case s: JsString =>
              if (legalPairs.nonEmpty) s.value.foreach { c =>
                if (!legalPairs.exists {
                      case (start, end) => c >= start && c <= end
                    })
                  throw DeserializationException(
                    s"""the \"${s.value}\" does not be accepted by legal charsets:${legalPairs.mkString(",")}""",
                    fieldNames = List(key))
              }
              if (s.value.length > lengthLimit)
                throw DeserializationException(s"the length of $s exceeds $lengthLimit", fieldNames = List(key))
            case _ =>
              throw DeserializationException(
                s"""the value mapped to \"$key\" must be String type""",
                fieldNames = List(key)
              )
          }
        )
      }
      this
    }

  /**
    * add your custom check for specific (key, value).
    *
    * Noted: we recommend you to throw DeserializationException when the input value is illegal.
    * @param key key
    * @param checker checker
    * @return this refiner
    */
  def valueChecker(key: String, checker: JsValue => Unit): JsonRefiner[T]

  /**
    * add your custom check for all keys
    * @param checker checker
    * @return this refiner
    */
  def keysChecker(checker: Set[String] => Unit): JsonRefiner[T]

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
            throw DeserializationException(s"""the \"${s.value}\" can't be converted to number""",
                                           e,
                                           fieldNames = List(key))
        }
      case s: JsValue => s
    }
  )

  protected def valueConverter(key: String, converter: JsValue => JsValue): JsonRefiner[T]

  def refine: OharaJsonFormat[T]
}

object JsonRefiner {

  trait StringRestriction[T] {
    private[this] var legalPair: Seq[(Char, Char)] = Seq.empty
    private[this] var lengthLimit: Int = Int.MaxValue

    /**
      * accept [a-zA-Z]
      * @return this restriction
      */
    def withCharset(): StringRestriction[T] = {
      withLowerCase()
      withUpperCase()
    }

    /**
      * accept [a-z]
      * @return this restriction
      */
    def withLowerCase(): StringRestriction[T] = withPair('a', 'z')

    /**
      * accept [A-Z]
      * @return this restriction
      */
    def withUpperCase(): StringRestriction[T] = withPair('A', 'Z')

    /**
      * accept [0-9]
      * @return this restriction
      */
    def withNumber(): StringRestriction[T] = withPair('0', '9')

    /**
      * accept [.]
      * @return this restriction
      */
    def withDot(): StringRestriction[T] = withPair('.', '.')

    /**
      * accept [-]
      * @return this restriction
      */
    def withDash(): StringRestriction[T] = withPair('-', '-')

    /**
      * accept [_]
      * @return this restriction
      */
    def withUnderLine(): StringRestriction[T] = withPair('_', '_')

    def withLengthLimit(limit: Int): StringRestriction[T] = {
      this.lengthLimit = CommonUtils.requirePositiveInt(limit)
      this
    }

    /**
      * Complete this restriction and add it to string refiner.
      */
    def toRefiner: JsonRefiner[T] = if (legalPair.isEmpty && lengthLimit == Int.MaxValue)
      throw new IllegalArgumentException("Don't use String Restriction if you hate to add any restriction")
    else addToJsonRefiner(legalPair, lengthLimit)

    /**
      * add custom regex to this restriction instance.
      * Noted: null and empty string produce exception.
      * @param startChar legal start char
      * @param endChar legal end char
      * @return this restriction
      */
    private[this] def withPair(startChar: Char, endChar: Char): StringRestriction[T] = {
      if (startChar > endChar)
        throw new IllegalArgumentException(s"the start char:$startChar must be small than end char:$endChar")
      legalPair = legalPair ++ Seq((startChar, endChar))
      this
    }

    protected def addToJsonRefiner(legalPairs: Seq[(Char, Char)], lengthLimit: Int): JsonRefiner[T]
  }

  trait ArrayRestriction[T] {
    private[this] var checkers: Seq[(String, JsArray) => Unit] = Seq.empty

    /**
      * throw exception if the value of this key equals to specific keyword
      * @param keyword the checked keyword
      * @return this refiner
      */
    def rejectKeyword(keyword: String): ArrayRestriction[T] = addChecker(
      (key, arr) =>
        if (arr.elements.exists(_.asInstanceOf[JsString].value == keyword))
          throw DeserializationException(s"""the \"$keyword\" is a illegal word to $key!!!"""))

    /**
      * throw exception if this key is empty array.
      * @return this refiner
      */
    def rejectEmpty(): ArrayRestriction[T] = addChecker(
      (key, arr) =>
        if (arr.elements.isEmpty)
          throw DeserializationException(s"""$key cannot be an empty array!!!"""))

    /**
      * Complete this restriction and add it to refiner.
      */
    def toRefiner: JsonRefiner[T] = if (checkers.isEmpty)
      throw new IllegalArgumentException("Don't use Array Restriction if you hate to add any restriction")
    else addToJsonRefiner(checkers)

    private[this] def addChecker(checker: (String, JsArray) => Unit): ArrayRestriction[T] = {
      this.checkers :+= checker
      this
    }

    protected def addToJsonRefiner(checkers: Seq[(String, JsArray) => Unit]): JsonRefiner[T]
  }

  def apply[T]: JsonRefiner[T] = new JsonRefiner[T] {
    private[this] var format: RootJsonFormat[T] = _
    private[this] var valueConverters: Map[String, JsValue => JsValue] = Map.empty
    private[this] var valueCheckers: Map[String, JsValue => Unit] = Map.empty
    private[this] var keyCheckers: Seq[Set[String] => Unit] = Seq.empty
    private[this] var nullToJsValue: Map[String, () => JsValue] = Map.empty
    private[this] var nullToAnotherValueOfKey: Map[String, String] = Map.empty
    private[this] var _rejectEmptyString: Boolean = false
    private[this] var _rejectNegativeNumber: Boolean = false
    private[this] var _rejectEmptyArray: Boolean = false

    override def format(format: RootJsonFormat[T]): JsonRefiner[T] = {
      this.format = Objects.requireNonNull(format)
      this
    }

    override def keysChecker(checker: Set[String] => Unit): JsonRefiner[T] = {
      this.keyCheckers = this.keyCheckers ++ Seq(checker)
      this
    }

    override def valueChecker(key: String, checker: JsValue => Unit): JsonRefiner[T] = {
      if (valueCheckers.contains(CommonUtils.requireNonEmpty(key)))
        throw new IllegalArgumentException(s"""the \"$key\" already has checker""")
      this.valueCheckers = this.valueCheckers ++ Map(key -> Objects.requireNonNull(checker))
      this
    }

    override protected def valueConverter(key: String, converter: JsValue => JsValue): JsonRefiner[T] = {
      if (valueConverters.contains(CommonUtils.requireNonEmpty(key)))
        throw new IllegalArgumentException(s"""the \"$key\" already has converter""")
      this.valueConverters = this.valueConverters ++ Map(key -> Objects.requireNonNull(converter))
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
        throw new IllegalArgumentException(s"""the \"$key\" has been associated to another key:\"$anotherKey\"""")
      this.nullToAnotherValueOfKey = this.nullToAnotherValueOfKey ++ Map(key -> CommonUtils.requireNonEmpty(anotherKey))
      this
    }

    override protected def nullToJsValue(key: String, defaultValue: () => JsValue): JsonRefiner[T] = {
      if (nullToJsValue.contains(CommonUtils.requireNonEmpty(key)))
        throw new IllegalArgumentException(s"""the \"$key\" have been associated to default value""")
      this.nullToJsValue = this.nullToJsValue ++ Map(key -> Objects.requireNonNull(defaultValue))
      this
    }

    override def refine: OharaJsonFormat[T] = {
      Objects.requireNonNull(format)
      nullToJsValue.keys.foreach(CommonUtils.requireNonEmpty)
      valueConverters.keys.foreach(CommonUtils.requireNonEmpty)
      nullToAnotherValueOfKey.keys.foreach(CommonUtils.requireNonEmpty)
      nullToAnotherValueOfKey.values.foreach(CommonUtils.requireNonEmpty)
      new OharaJsonFormat[T] {

        override def check[Value <: JsValue](key: String, value: Value): Value = {
          def checkEmptyString(k: String, s: JsString): Unit =
            if (s.value.isEmpty)
              throw DeserializationException(s"""the value of \"$k\" can't be empty string!!!""", fieldNames = List(k))
          def checkJsValueForEmptyString(k: String, v: JsValue): Unit = v match {
            case s: JsString => checkEmptyString(k, s)
            case s: JsArray  => s.elements.foreach(v => checkJsValueForEmptyString(k, v))
            case s: JsObject => s.fields.foreach(pair => checkJsValueForEmptyString(pair._1, pair._2))
            case _           => // nothing
          }

          // 1) check empty string
          if (_rejectEmptyString) checkJsValueForEmptyString(key, value)

          def checkNegativeNumber(k: String, s: JsNumber): Unit = if (s.value < 0)
            throw DeserializationException(s"""the \"${s.value}\" of \"$k\" MUST be bigger than or equal to zero!!!""",
                                           fieldNames = List(k))
          def checkJsValueForNegativeNumber(k: String, v: JsValue): Unit = v match {
            case s: JsNumber => checkNegativeNumber(k, s)
            case s: JsArray  => s.elements.foreach(v => checkJsValueForNegativeNumber(k, v))
            case s: JsObject =>
              s.fields.foreach(pair => checkJsValueForNegativeNumber(pair._1, pair._2))
            case _ => // nothing
          }

          // 2) check negative number
          if (_rejectNegativeNumber) checkJsValueForNegativeNumber(key, value)

          def checkEmptyArray(k: String, s: JsArray): Unit = if (s.elements.isEmpty)
            throw DeserializationException(s"""the value of \"$k\" MUST be NOT empty array!!!""", fieldNames = List(k))
          def checkJsValueForEmptyArray(k: String, v: JsValue): Unit = v match {
            case s: JsArray => checkEmptyArray(k, s)
            case s: JsObject =>
              s.fields.foreach(pair => checkJsValueForEmptyArray(pair._1, pair._2))
            case _ => // nothing
          }

          // 3) check empty array
          if (_rejectEmptyArray) checkJsValueForEmptyArray(key, value)

          // 4) custom check
          valueCheckers.get(key).foreach(_.apply(value))
          value
        }

        override def read(json: JsValue): T = {
          var fields = json.asJsObject.fields.filter {
            case (_, value) =>
              value match {
                case JsNull => false
                case _      => true
              }
          }

          // 1) convert the value to another type
          fields = fields ++ valueConverters
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
              key -> fields.getOrElse(key, defaultValue())
          }

          // 4) check the value
          fields.foreach {
            case (k, v) => check(k, v)
          }

          // 5) check the keys
          keyCheckers.foreach(_(fields.keySet))

          format.read(JsObject(fields))
        }
        override def write(obj: T): JsValue = format.write(obj)
      }
    }
  }
}
