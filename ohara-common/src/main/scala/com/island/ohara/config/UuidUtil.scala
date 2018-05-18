package com.island.ohara.config

/**
  * A helper used to create a uuid string.
  */
object UuidUtil {

  /**
    * create a uuid.
    * @return uuid
    */
  def uuid(): String = java.util.UUID.randomUUID.toString
}
