package com.island.ohara.client.configurator

/**
  * Restful APIs of configurator is a part of public APIs of ohara. In order to keep our compatibility guarantee, we
  * put "version" to all urls of APIs. All constant strings are written in this class. DON'T change them arbitrarily since
  * any change to the constant string will break the compatibility!!!
  */
object ConfiguratorApiInfo {

  /**
    * Our first version of APIs!!!
    */
  val V0: String = "v0"

  /**
    * configurator has some private APIs used in testing. The url of APIs are located at this "private" scope.
    */
  val PRIVATE: String = "_private"
}
