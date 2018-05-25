package com.island.ohara.configurator.serialization

import com.island.ohara.config.{OharaConfig, OharaJson}
import com.island.ohara.configurator.data.OharaData
import com.island.ohara.io.ByteUtil
import com.island.ohara.reflection.ReflectionUtil

class OharaDataSerializer extends Serializer[OharaData, Array[Byte]] {

  /**
    * Convert the OharaData to a serializable type.
    *
    * @param obj OharaData
    * @return a serializable type
    */
  override def to(obj: OharaData): Array[Byte] = ByteUtil.toBytes(obj.toJson(false).asString)

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
      .instantiate(OharaData.implProperty.require(config), classOf[OharaData], (classOf[OharaConfig], config))
  }
}

object OharaDataSerializer {
  val SINGLETON = new OharaDataSerializer()
}
