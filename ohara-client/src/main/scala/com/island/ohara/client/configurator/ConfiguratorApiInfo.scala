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
