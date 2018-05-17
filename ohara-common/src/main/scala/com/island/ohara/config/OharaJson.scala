package com.island.ohara.config

/**
  * Just a annotation to represent a json object. We won't have our json parser/serializer since it is a difficult works. Nevertheless, using
  * string to represent is not a good way since it is hard to distinuish the string and "json" string.
  */
trait OharaJson {
  def asString: String
}

object OharaJson {
  def apply(json: String): OharaJson = new OharaJson() {
    override def asString: String = json
  }
}
