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

package oharastream.ohara.client.configurator.v0

import java.util.Objects

import oharastream.ohara.client.configurator.v0.JsonFormatBuilder.ArrayRestriction
import oharastream.ohara.common.setting.SettingDef.{Necessary, Permission, Type}
import oharastream.ohara.common.setting.{ObjectKey, PropGroup, SettingDef}
import oharastream.ohara.common.util.CommonUtils
import spray.json.DefaultJsonProtocol._
import spray.json.{
  DeserializationException,
  JsArray,
  JsBoolean,
  JsNull,
  JsNumber,
  JsObject,
  JsString,
  JsValue,
  RootJsonFormat
}

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.reflect.{ClassTag, classTag}

/**
  * this is a akka-json representation++ which offers many useful sugar conversion of input json.
  * The order of applying rules is shown below.
  * 1) convert the value to another type
  * 2) convert the null to the value of another key
  * 3) convert the null to default value
  * 4) value check
  *
  * Noted: the keys having checker MUST exist. otherwise, DeserializationException will be thrown to interrupt the serialization.
  * @tparam T scala object type
  */
trait JsonFormatBuilder[T] extends oharastream.ohara.common.pattern.Builder[JsonFormat[T]] {
  def format(format: RootJsonFormat[T]): JsonFormatBuilder[T]

  /**
    * config this refiner according to setting definitions
    * @param definitions setting definitions
    * @return this refiner
    */
  def definitions(definitions: Seq[SettingDef]): JsonFormatBuilder[T] = {
    definitions.foreach(definition)
    this
  }

  /**
    * config this refiner according to setting definition
    * @param definition setting definition
    * @return this refiner
    */
  def definition(definition: SettingDef): JsonFormatBuilder[T] = {
    // the internal is not exposed to user so we skip it.
    // remove the value if the field is readonly
    if (definition.permission() == Permission.READ_ONLY) valueConverter(definition.key(), _ => JsNull)
    definition.regex().ifPresent(regex => stringRestriction(definition.key(), regex))
    if (!definition.internal() && definition.necessary() == Necessary.REQUIRED) requireKey(definition.key())
    definition.valueType() match {
      case Type.BOOLEAN =>
        if (definition.hasDefault)
          nullToBoolean(definition.key(), definition.defaultBoolean)
        if (!definition.internal()) requireJsonType[JsBoolean](definition.key())
      case Type.POSITIVE_SHORT =>
        if (definition.hasDefault)
          nullToShort(definition.key(), definition.defaultShort)
        if (!definition.internal()) requireNumberType(key = definition.key(), min = 1, max = Short.MaxValue)
      case Type.SHORT =>
        if (definition.hasDefault)
          nullToShort(definition.key(), definition.defaultShort)
        if (!definition.internal())
          requireNumberType(key = definition.key(), min = Short.MinValue, max = Short.MaxValue)
      case Type.POSITIVE_INT =>
        if (definition.hasDefault) nullToInt(definition.key(), definition.defaultInt)
        if (!definition.internal()) requireNumberType(key = definition.key(), min = 1, max = Int.MaxValue)
      case Type.INT =>
        if (definition.hasDefault) nullToInt(definition.key(), definition.defaultInt)
        if (!definition.internal()) requireNumberType(key = definition.key(), min = Int.MinValue, max = Int.MaxValue)
      case Type.POSITIVE_LONG =>
        if (definition.hasDefault) nullToLong(definition.key(), definition.defaultLong)
        if (!definition.internal()) requireNumberType(key = definition.key(), min = 1, max = Long.MaxValue)
      case Type.LONG =>
        if (definition.hasDefault) nullToLong(definition.key(), definition.defaultLong)
        if (!definition.internal()) requireNumberType(key = definition.key(), min = Long.MinValue, max = Long.MaxValue)
      case Type.POSITIVE_DOUBLE =>
        if (definition.hasDefault)
          nullToDouble(definition.key(), definition.defaultDouble)
        if (!definition.internal()) requireNumberType(key = definition.key(), min = 1, max = Double.MaxValue)
      case Type.DOUBLE =>
        if (definition.hasDefault)
          nullToDouble(definition.key(), definition.defaultDouble)
        if (!definition.internal())
          requireNumberType(key = definition.key(), min = Double.MinValue, max = Double.MaxValue)
      case Type.ARRAY =>
        // we don't allow to set default value to array type.
        // see SettingDef
        if (definition.hasDefault)
          throw new IllegalArgumentException(
            "the default value to array type is not allowed. default:" + definition.defaultValue
          )
        if (!definition.internal()) {
          nullToJsValue(definition.key(), () => JsArray.empty)
          definition.necessary() match {
            case SettingDef.Necessary.REQUIRED =>
              requireJsonType[JsArray](
                definition.key(),
                (array: JsArray) =>
                  if (array.elements.isEmpty)
                    throw DeserializationException(s"empty array is illegal", fieldNames = List(definition.key()))
              )
            case _ =>
              requireJsonType[JsArray](definition.key())
          }
          // check black list
          requireJsonType[JsArray](
            definition.key(),
            (array: JsArray) => {
              array.elements.foreach {
                case JsString(s) =>
                  if (definition.blacklist().asScala.contains(s))
                    throw DeserializationException(s"the $s is protected word", fieldNames = List(definition.key()))
                case _ => // we only check the string value
              }
            }
          )
        }
      case Type.DURATION =>
        if (definition.hasDefault) nullToString(definition.key(), definition.defaultDuration.toString)
        if (!definition.internal()) requireType[Duration](definition.key())
      case Type.REMOTE_PORT =>
        if (definition.hasDefault)
          nullToShort(definition.key(), definition.defaultShort)
        if (!definition.internal()) requireConnectionPort(definition.key())
      case Type.BINDING_PORT =>
        if (definition.hasDefault)
          nullToShort(definition.key(), definition.defaultShort)
        else if (definition.necessary() == Necessary.RANDOM_DEFAULT)
          nullToJsValue(definition.key(), () => JsNumber(CommonUtils.availablePort()))
        if (!definition.internal()) requireConnectionPort(definition.key())
      case Type.OBJECT_KEY =>
        // we don't allow to set default value to array type.
        // see SettingDef
        if (definition.hasDefault)
          throw new IllegalArgumentException(
            "the default value to object key is not allowed. default:" + definition.defaultValue()
          )
        if (!definition.internal()) {
          // convert the value to complete Object key
          valueConverter(definition.key(), v => OBJECT_KEY_FORMAT.write(OBJECT_KEY_FORMAT.read(v)))
          requireType[ObjectKey](definition.key())
        }
      case Type.OBJECT_KEYS =>
        // we don't allow to set default value to array type.
        // see SettingDef
        if (definition.hasDefault)
          throw new IllegalArgumentException(
            "the default value to array type is not allowed. default:" + definition.defaultValue()
          )
        if (!definition.internal()) {
          nullToJsValue(definition.key(), () => JsArray.empty)
          // convert the value to complete Object key
          valueConverter(
            definition.key(), {
              case JsArray(es) => JsArray(es.map(v => OBJECT_KEY_FORMAT.write(OBJECT_KEY_FORMAT.read(v))))
              case v: JsValue  => OBJECT_KEY_FORMAT.write(OBJECT_KEY_FORMAT.read(v))
            }
          )
          definition.necessary() match {
            case SettingDef.Necessary.REQUIRED =>
              requireType[Seq[ObjectKey]](
                definition.key(),
                (keys: Seq[ObjectKey]) =>
                  if (keys.isEmpty)
                    throw DeserializationException(s"empty keys is illegal", fieldNames = List(definition.key()))
              )
            case _ =>
              requireType[Seq[ObjectKey]](definition.key())
          }
        }
      case Type.TAGS =>
        // we don't allow to set default value to array type.
        // see SettingDef
        if (definition.hasDefault)
          throw new IllegalArgumentException(
            "the default value to array type is not allowed. default:" + definition.defaultValue()
          )
        if (!definition.internal()) {
          nullToJsValue(definition.key(), () => JsObject.empty)
          definition.necessary() match {
            case SettingDef.Necessary.REQUIRED =>
              requireJsonType[JsObject](
                definition.key(),
                (tags: JsObject) =>
                  if (tags.fields.isEmpty)
                    throw DeserializationException(s"empty tags is illegal", fieldNames = List(definition.key()))
              )
            case _ =>
              requireJsonType[JsObject](definition.key())
          }
        }
      case Type.TABLE =>
        // we don't allow to set default value to array type.
        // see SettingDef
        if (definition.hasDefault)
          throw new IllegalArgumentException(
            "the default value to array type is not allowed. default:" + definition.defaultValue()
          )
        if (!definition.internal()) {
          nullToJsValue(definition.key(), () => JsArray.empty)
          definition.necessary() match {
            case SettingDef.Necessary.REQUIRED =>
              requireType[PropGroup](
                definition.key(),
                (table: PropGroup) =>
                  if (table.raw().isEmpty)
                    throw DeserializationException(s"empty table is illegal", fieldNames = List(definition.key()))
              )
            case _ =>
              requireType[PropGroup](definition.key())
          }
        }
      case Type.STRING =>
        if (definition.hasDefault) nullToString(definition.key(), definition.defaultString)
        else if (definition.necessary() == Necessary.RANDOM_DEFAULT)
          nullToJsValue(
            definition.key(),
            () =>
              JsString(
                definition.prefix().orElse("") + CommonUtils.randomString(SettingDef.STRING_LENGTH_LIMIT)
              )
          )
        if (!definition.internal()) requireJsonType[JsString](definition.key())
      case _ @(Type.CLASS | Type.PASSWORD | Type.JDBC_TABLE) =>
        if (definition.hasDefault) nullToString(definition.key(), definition.defaultString)
        if (!definition.internal()) requireJsonType[JsString](definition.key())
    }
    this
  }
  //-------------------------[null to default]-------------------------//

  def nullToBoolean(key: String, value: Boolean): JsonFormatBuilder[T] = nullToJsValue(key, () => JsBoolean(value))

  def nullToString(key: String, defaultValue: String): JsonFormatBuilder[T] =
    nullToJsValue(key, () => JsString(defaultValue))

  def nullToString(key: String, defaultValue: () => String): JsonFormatBuilder[T] =
    nullToJsValue(key, () => JsString(defaultValue()))

  def nullToEmptyArray(key: String): JsonFormatBuilder[T] = nullToJsValue(key, () => JsArray.empty)

  def nullToEmptyObject(key: String): JsonFormatBuilder[T] = nullToJsValue(key, () => JsObject.empty)

  def nullToShort(key: String, value: Short): JsonFormatBuilder[T] = nullToJsValue(key, () => JsNumber(value))

  def nullToInt(key: String, value: Int): JsonFormatBuilder[T] = nullToJsValue(key, () => JsNumber(value))

  def nullToLong(key: String, value: Long): JsonFormatBuilder[T] = nullToJsValue(key, () => JsNumber(value))

  def nullToDouble(key: String, value: Double): JsonFormatBuilder[T] = nullToJsValue(key, () => JsNumber(value))

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
  def nullToAnotherValueOfKey(key: String, anotherKey: String): JsonFormatBuilder[T]

  //-------------------------[more checks]-------------------------//

  /**
    * require the key for input json. It produces exception if input json is lack of key.
    * @param key required key
    * @return this refiner
    */
  def requireKey(key: String): JsonFormatBuilder[T] = requireKeys(Set(key))

  /**
    * require the keys for input json. It produces exception if input json is lack of those keys.
    * @param keys required keys
    * @return this refiner
    */
  def requireKeys(keys: Set[String]): JsonFormatBuilder[T] = keysChecker { inputKeys =>
    val nonexistentKeys = keys.diff(inputKeys)
    if (nonexistentKeys.nonEmpty)
      throw DeserializationException(
        s"${nonexistentKeys.mkString(",")} are required!!!",
        fieldNames = nonexistentKeys.toList
      )
  }

  /**
    * require the sum length of passing keys' values should fit the length limit.
    * The required keys should exist in request data.
    * Note: We only accept string type.
    *
    * @param keys to be checked keys
    * @param sumLengthLimit sum length limit
    * @return this refiner
    */
  def stringSumLengthLimit(keys: Set[String], sumLengthLimit: Int): JsonFormatBuilder[T] =
    // check the keys should exist in request
    requireKeys(keys).valuesChecker(
      keys,
      fields => {
        val sum = fields.values.map {
          case s: JsString => s.value.length
          case _           => throw DeserializationException(s"we only support length checking for string value")
        }.sum
        if (sum > sumLengthLimit)
          throw DeserializationException(s"the length of $sum exceeds $sumLengthLimit", fieldNames = keys.toList)
      }
    )

  /**
    * check whether target port is legal to connect. The legal range is (0, 65535].
    * @param key key
    * @return this refiner
    */
  def requireConnectionPort(key: String): JsonFormatBuilder[T] = requireNumberType(key = key, min = 1, max = 65535)

  /**
    * require the number type and check the legal number.
    * @param key key
    * @param min the min value (included). if input value is bigger than min, DeserializationException is thrown
    * @param max the max value (included). if input value is smaller than max, DeserializationException is thrown
    * @return this refiner
    */
  def requireNumberType(key: String, min: Long, max: Long): JsonFormatBuilder[T] = requireJsonType[JsNumber](
    key,
    (jsNumber: JsNumber) => {
      val number = jsNumber.value.toLong
      if (number < min || number > max)
        throw DeserializationException(s"$key: the number must be [$min, $max], actual:$number")
    }
  )

  /**
    * require the number type and check the legal number.
    * @param key key
    * @param min the min value (included). if input value is bigger than min, DeserializationException is thrown
    * @param max the max value (included). if input value is smaller than max, DeserializationException is thrown
    * @return this refiner
    */
  private[this] def requireNumberType(key: String, min: Double, max: Double): JsonFormatBuilder[T] =
    requireJsonType[JsNumber](
      key,
      (jsNumber: JsNumber) => {
        val number = jsNumber.value.toDouble
        if (number < min || number > max)
          throw DeserializationException(s"$key: the number must be [$min, $max], actual:$number")
      }
    )

  /**
    * check the value of existent key. the value type MUST match the expected type. otherwise, the DeserializationException is thrown.
    * @param key key
    * @tparam Json expected value type
    * @return this refiner
    */
  private[this] def requireJsonType[Json <: JsValue: ClassTag](key: String): JsonFormatBuilder[T] =
    requireJsonType(key, (_: Json) => Unit)

  /**
    * check the value of existent key. the value type MUST match the expected type. otherwise, the DeserializationException is thrown.
    * @param key key
    * @param checker checker
    * @tparam Json expected value type
    * @return this refiner
    */
  private[this] def requireJsonType[Json <: JsValue: ClassTag](
    key: String,
    checker: Json => Unit
  ): JsonFormatBuilder[T] =
    valueChecker(
      key,
      json => {
        if (!classTag[Json].runtimeClass.isInstance(json))
          throw DeserializationException(
            s"""the $key must be ${classTag[Json].runtimeClass.getSimpleName} type, but actual type is \"${json.getClass.getSimpleName}\"""",
            fieldNames = List(key)
          )
        checker(json.asInstanceOf[Json])
      }
    )

  /**
    * check the value of existent key. the value type MUST match the expected type. otherwise, the DeserializationException is thrown.
    * @param key key
    * @tparam C expected value type
    * @return this refiner
    */
  private[this] def requireType[C](key: String)(implicit format: RootJsonFormat[C]): JsonFormatBuilder[T] =
    requireType(key, (_: C) => Unit)

  /**
    * check the value of existent key. the value type MUST match the expected type. otherwise, the DeserializationException is thrown.
    * @param key key
    * @param checker checker
    * @return this refiner
    */
  private[this] def requireType[C](key: String, checker: C => Unit)(
    implicit format: RootJsonFormat[C]
  ): JsonFormatBuilder[T] =
    valueChecker(
      key,
      json => checker(format.read(json))
    )

  /**
    * reject the request having a key which is associated to empty string.
    * Noted: this rule is applied to all key-value even if the pair is in nested object.
    * @return this refiner
    */
  def rejectEmptyString(): JsonFormatBuilder[T]

  /**
    * throw exception if the specific key of input json is associated to empty string.
    * This method check only the specific key. By contrast, rejectEmptyString() checks values for all keys.
    * @param key key
    * @return this refiner
    */
  def rejectEmptyString(key: String): JsonFormatBuilder[T] = valueChecker(
    key, {
      case JsString(s) if s.isEmpty =>
        throw DeserializationException(s"""the value of \"$key\" can't be empty string!!!""", fieldNames = List(key))
      case _ => // we don't care for other types
    }
  )

  /**
    * throw exception if the input json has empty array.
    * Noted: this rule is applied to all key-value even if the pair is in nested object.
    * @return this refiner
    */
  def rejectEmptyArray(): JsonFormatBuilder[T]

  /**
    * throw exception if the specific key of input json is associated to empty array.
    * @param key key
    * @return this refiner
    */
  def rejectEmptyArray(key: String): JsonFormatBuilder[T] = arrayRestriction(key).rejectEmpty().toRefiner

  /**
    * add the array restriction to specific value.
    * It throws exception if the specific key of input json violates any restriction.
    * Noted: the empty restriction make the checker to reject all input values.
    * @param key key
    * @return this refiner
    */
  def arrayRestriction(key: String): ArrayRestriction[T] =
    (checkers: Seq[(String, JsArray) => Unit]) =>
      requireJsonType[JsArray](key, (jsArray: JsArray) => checkers.foreach(_.apply(key, jsArray)))

  def stringRestriction(key: String, regex: String): JsonFormatBuilder[T] = valueChecker(
    key,
    (v: JsValue) => {
      val s = v match {
        case JsString(s) => s
        case _           => v.toString
      }
      if (!s.matches(regex))
        throw DeserializationException(s"""the value \"$s\" is not matched to $regex""", fieldNames = List(key))
    }
  )

  /**
    * add your custom check for specific (key, value). the checker is executed only if the key exists.
    *
    * Noted: we recommend you to throw DeserializationException when the input value is illegal.
    * @param key key
    * @param checker checker
    * @return this refiner
    */
  def valueChecker(key: String, checker: JsValue => Unit): JsonFormatBuilder[T] =
    valuesChecker(Set(CommonUtils.requireNonEmpty(key)), vs => checker(vs.head._2))

  //-------------------------[more conversion]-------------------------//

  /**
    * Set the auto-conversion to key that the string value wiil be converted to number. Hence, the key is able to accept
    * both string type and number type. By contrast, akka json will produce DeserializationException in parsing string to number.
    * @param key key
    * @return this refiner
    */
  def acceptStringToNumber(key: String): JsonFormatBuilder[T] = valueConverter(
    key, {
      case s: JsString =>
        try JsNumber(s.value)
        catch {
          case e: NumberFormatException =>
            throw DeserializationException(
              s"""the \"${s.value}\" can't be converted to number""",
              e,
              fieldNames = List(key)
            )
        }
      case s: JsValue => s
    }
  )

  //-------------------------[protected methods]-------------------------//

  protected def keysChecker(checker: Set[String] => Unit): JsonFormatBuilder[T]

  /**
    * set the default number for input keys. The default value will be added into json request if the associated key is nonexistent.
    * @param key key
    * @param defaultValue default value
    * @return this refiner
    */
  protected def nullToJsValue(key: String, defaultValue: () => JsValue): JsonFormatBuilder[T]

  /**
    * add your custom check for a set of keys' values.
    * Noted: We don't guarantee the order of each (key, value) pair since this method is intend
    * to do an "aggregation" check, like sum or length check.
    *
    * @param keys keys
    * @param checker checker
    * @return this refiner
    */
  protected def valuesChecker(keys: Set[String], checker: Map[String, JsValue] => Unit): JsonFormatBuilder[T]

  protected def valueConverter(key: String, converter: JsValue => JsValue): JsonFormatBuilder[T]

  override def build: JsonFormat[T]
}

object JsonFormatBuilder {
  trait ArrayRestriction[T] {
    private[this] var checkers: Seq[(String, JsArray) => Unit] = Seq.empty

    /**
      * throw exception if the value of this key equals to specific keyword
      * @param keyword the checked keyword
      * @return this refiner
      */
    def rejectKeyword(keyword: String): ArrayRestriction[T] =
      addChecker(
        (key, arr) =>
          if (arr.elements.exists(_.asInstanceOf[JsString].value == keyword))
            throw DeserializationException(s"""the \"$keyword\" is a illegal word to $key!!!""")
      )

    /**
      * throw exception if this key is empty array.
      * @return this refiner
      */
    def rejectEmpty(): ArrayRestriction[T] =
      addChecker(
        (key, arr) =>
          if (arr.elements.isEmpty)
            throw DeserializationException(s"""$key cannot be an empty array!!!""")
      )

    /**
      * Complete this restriction and add it to refiner.
      */
    def toRefiner: JsonFormatBuilder[T] =
      if (checkers.isEmpty)
        throw new IllegalArgumentException("Don't use Array Restriction if you hate to add any restriction")
      else addToJsonRefiner(checkers)

    private[this] def addChecker(checker: (String, JsArray) => Unit): ArrayRestriction[T] = {
      this.checkers :+= checker
      this
    }

    protected def addToJsonRefiner(checkers: Seq[(String, JsArray) => Unit]): JsonFormatBuilder[T]
  }

  def apply[T]: JsonFormatBuilder[T] = new JsonFormatBuilder[T] {
    private[this] var format: RootJsonFormat[T]                                      = _
    private[this] var valueConverters: Map[String, JsValue => JsValue]               = Map.empty
    private[this] var keyCheckers: Seq[Set[String] => Unit]                          = Seq.empty
    private[this] var valuesCheckers: Map[Set[String], Map[String, JsValue] => Unit] = Map.empty
    private[this] var nullToJsValue: Map[String, () => JsValue]                      = Map.empty
    private[this] var nullToAnotherValueOfKey: Map[String, String]                   = Map.empty
    private[this] var _rejectEmptyString: Boolean                                    = false
    private[this] var _rejectEmptyArray: Boolean                                     = false

    override def format(format: RootJsonFormat[T]): JsonFormatBuilder[T] = {
      this.format = Objects.requireNonNull(format)
      this
    }
    override protected def keysChecker(checker: Set[String] => Unit): JsonFormatBuilder[T] = {
      this.keyCheckers = this.keyCheckers ++ Seq(checker)
      this
    }

    override protected def valuesChecker(
      keys: Set[String],
      checkers: Map[String, JsValue] => Unit
    ): JsonFormatBuilder[T] = {
      /**
        * compose the new checker with older one.
        */
      val composedChecker = valuesCheckers
        .get(keys)
        .map(
          origin =>
            (fields: Map[String, JsValue]) => {
              origin(fields)
              checkers(fields)
            }
        )
        .getOrElse(checkers)
      this.valuesCheckers = this.valuesCheckers + (keys -> Objects.requireNonNull(composedChecker))
      this
    }

    override protected def valueConverter(key: String, converter: JsValue => JsValue): JsonFormatBuilder[T] = {
      /**
        * compose the new checker with older one.
        */
      val composedChecker = valueConverters
        .get(key)
        .map(
          origin =>
            (value: JsValue) => {
              converter(origin(value))
            }
        )
        .getOrElse(converter)
      this.valueConverters = this.valueConverters + (key -> Objects.requireNonNull(composedChecker))
      this
    }

    override def rejectEmptyString(): JsonFormatBuilder[T] = {
      this._rejectEmptyString = true
      this
    }

    override def rejectEmptyArray(): JsonFormatBuilder[T] = {
      this._rejectEmptyArray = true
      this
    }

    override def nullToAnotherValueOfKey(key: String, anotherKey: String): JsonFormatBuilder[T] = {
      if (nullToAnotherValueOfKey.contains(CommonUtils.requireNonEmpty(key)))
        throw new IllegalArgumentException(s"""the \"$key\" has been associated to another key:\"$anotherKey\"""")
      this.nullToAnotherValueOfKey = this.nullToAnotherValueOfKey + (key -> CommonUtils.requireNonEmpty(anotherKey))
      this
    }

    override protected def nullToJsValue(key: String, defaultValue: () => JsValue): JsonFormatBuilder[T] = {
      if (nullToJsValue.contains(CommonUtils.requireNonEmpty(key)))
        throw new IllegalArgumentException(
          s"""the \"$key\" have been associated to default value:${nullToJsValue(key)}"""
        )
      this.nullToJsValue = this.nullToJsValue + (key -> Objects.requireNonNull(defaultValue))
      this
    }

    override def build: JsonFormat[T] = {
      Objects.requireNonNull(format)
      nullToJsValue.keys.foreach(CommonUtils.requireNonEmpty)
      valueConverters.keys.foreach(CommonUtils.requireNonEmpty)
      nullToAnotherValueOfKey.keys.foreach(CommonUtils.requireNonEmpty)
      nullToAnotherValueOfKey.values.foreach(CommonUtils.requireNonEmpty)
      new JsonFormat[T] {
        override def more(definitions: Seq[SettingDef]): JsonFormat[T] =
          JsonFormatBuilder[T]
            .format(this)
            .definitions(definitions)
            .build

        private[this] def checkGlobalCondition(key: String, value: JsValue): Unit = {
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

          def checkEmptyArray(k: String, s: JsArray): Unit =
            if (s.elements.isEmpty)
              throw DeserializationException(
                s"""the value of \"$k\" MUST be NOT empty array!!!""",
                fieldNames = List(k)
              )

          def checkJsValueForEmptyArray(k: String, v: JsValue): Unit = v match {
            case s: JsArray => checkEmptyArray(k, s)
            case s: JsObject =>
              s.fields.foreach(pair => checkJsValueForEmptyArray(pair._1, pair._2))
            case _ => // nothing
          }

          // 2) check empty array
          if (_rejectEmptyArray) checkJsValueForEmptyArray(key, value)
        }

        override def read(json: JsValue): T = json match {
          // we refine only the complicated json object
          case _: JsObject =>
            var fields = json.asJsObject.fields.filter {
              case (_, value) =>
                value match {
                  case JsNull => false
                  case _      => true
                }
            }

            // 1) convert the value to another type
            fields = (fields ++ valueConverters
              .filter {
                case (key, _) => fields.contains(key)
              }
              .map {
                case (key, converter) => key -> converter(fields(key))
              }).filter {
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

            // 4) check the keys existence
            // this check is excluded from step.4 since the check is exposed publicly, And it does care for key-value only.
            keyCheckers.foreach(_(fields.keySet))

            // 5) check the fields
            fields = check(fields)

            format.read(JsObject(fields))
          case _ => format.read(json)
        }
        override def write(obj: T): JsValue = format.write(obj)

        override def check(fields: Map[String, JsValue]): Map[String, JsValue] = {
          // 1) check global condition
          fields.foreach {
            case (k, v) => checkGlobalCondition(k, v)
          }

          // 2) custom checks
          valuesCheckers.foreach {
            case (keys, checker) =>
              // we don't run the check if the keys are not matched
              val matchedFields = fields.filter(e => keys.contains(e._1))
              if (matchedFields.size == keys.size) checker(matchedFields)
          }

          fields
        }
      }
    }
  }
}
