package com.island.ohara.config

/**
  * Just a annotation to represent a json object. We won't have our json parser/serializer since it is a difficult works. Nevertheless, using
  * string to represent is not a good way since it is hard to distinguish the string and "json" string.
  */
trait OharaJson {
  override def toString: String

  override def equals(obj: scala.Any): Boolean = obj match {
    case other: OharaJson => toString.eq(other.toString)
    case _                => false
  }
}

object OharaJson {
  def apply(json: String): OharaJson = new OharaJson() {
    override def toString: String = json
  }
}
