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

package com.island.ohara

import com.island.ohara.common.util.CommonUtils
import spray.json._
package object agent {

  private[this] val KEY_TO_SETTINGS: String = "_key_to_settings_"

  /**
    * Convert all settings into a single string which is able to be saved in container envs.
    * All quotes in string are replaced by slash since the quote is ignored by env.
    * @param settings the settings will be stored
    * @return string key
    */
  def toEnvString(settings: Map[String, JsValue]): Map[String, String] = Map(
    KEY_TO_SETTINGS -> CommonUtils.toEnvString(JsObject(settings).toString()))

  /**
    * seek the internal key which stores all settings in a normal string.
    * @param envs all environment variables
    * @return settings
    */
  def toSettings(envs: Map[String, String]): Map[String, JsValue] = envs
    .get(KEY_TO_SETTINGS)
    .map(CommonUtils.fromEnvString(_).parseJson.asJsObject.fields)
    .getOrElse(throw new NoSuchElementException(s"the internal key:$KEY_TO_SETTINGS does not exist!!!"))
}
