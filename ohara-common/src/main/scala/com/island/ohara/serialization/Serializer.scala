package com.island.ohara.serialization

/**
  * Used to convert a T object to V
  * NOTED: the impl should not be an inner/anonymous class since Store will use the reflection to create the object. The dynamical
  * call to inner/anonymous class is fraught with risks.
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
  val STRING = StringSerializer
  val SHORT = ShortSerializer
  val INT = IntSerializer
  val LONG = LongSerializer
  val DOUBLE = DoubleSerializer
  val FLOAT = FloatSerializer
  val BYTES = BytesSerializer
  val BOOLEAN = BooleanSerializer
  val OHARA_DATA = OharaDataSerializer
  val OBJECT = ObjectSerializer
  val ROW = RowSerializer
}
