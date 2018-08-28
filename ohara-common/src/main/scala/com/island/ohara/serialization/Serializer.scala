package com.island.ohara.serialization
import com.island.ohara.data.Row

/**
  * Used to convert a T object to V
  * NOTED: the impl should not be an inner/anonymous class since Store will use the reflection to create the object. The dynamical
  * call to inner/anonymous class is fraught with risks.
  *
  * @tparam T object type
  */
trait Serializer[T] {

  /**
    * Convert the object to a serializable type
    * @param obj object
    * @return a serializable type
    */
  def to(obj: T): Array[Byte]

  /**
    * Convert the serialized data to object
    * @param serial serialized data
    * @return object
    */
  def from(serial: Array[Byte]): T
}

object Serializer {
  val STRING: Serializer[String] = StringSerializer
  val SHORT: Serializer[Short] = ShortSerializer
  val INT: Serializer[Int] = IntSerializer
  val LONG: Serializer[Long] = LongSerializer
  val DOUBLE: Serializer[Double] = DoubleSerializer
  val FLOAT: Serializer[Float] = FloatSerializer
  val BYTES: Serializer[Array[Byte]] = BytesSerializer
  val BOOLEAN: Serializer[Boolean] = BooleanSerializer
  val OBJECT: Serializer[Any] = ObjectSerializer
  val ROW: Serializer[Row] = RowSerializer
}
