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
  implicit val STRING: Serializer[String] = StringSerializer
  implicit val SHORT: Serializer[Short] = ShortSerializer
  implicit val INT: Serializer[Int] = IntSerializer
  implicit val LONG: Serializer[Long] = LongSerializer
  implicit val DOUBLE: Serializer[Double] = DoubleSerializer
  implicit val FLOAT: Serializer[Float] = FloatSerializer
  implicit val BYTES: Serializer[Array[Byte]] = BytesSerializer
  implicit val BOOLEAN: Serializer[Boolean] = BooleanSerializer
  implicit val OBJECT: Serializer[Any] = ObjectSerializer
  implicit val ROW: Serializer[Row] = RowSerializer
}
