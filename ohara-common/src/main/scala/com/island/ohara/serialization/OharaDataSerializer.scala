package com.island.ohara.serialization
import com.island.ohara.config.{OharaConfig, OharaJson}
import com.island.ohara.data.OharaData
import com.island.ohara.io.ByteUtil
import com.island.ohara.reflection.ReflectionUtil

/**
  * Used to do conversion between ohara data and byte array.
  */
object OharaDataSerializer extends Serializer[OharaData] {

  /**
    * Convert the OharaData to a serializable type.
    *
    * @param obj OharaData
    * @return a serializable type
    */
  override def to(obj: OharaData): Array[Byte] = ByteUtil.toBytes(obj.toJson.toString)

  /**
    * Convert the serialized data to OharaData.
    * Noted: we use the reflection to create the impl of OharaData. It is expensive but it enable us to add new impl
    * of OharaData without recompilation.
    *
    * @param serial serialized data
    * @return OharaData
    */
  override def from(serial: Array[Byte]): OharaData = {
    val config = OharaConfig(OharaJson(ByteUtil.toString(serial)))
    ReflectionUtil
      .instantiate(OharaData.IMPLEMENTATION_NAME.require(config), classOf[OharaData], (classOf[OharaConfig], config))
  }
}
