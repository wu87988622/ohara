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

package com.island.ohara.client.configurator.v0

import spray.json.JsValue

/**
  * This is the basic type which can be stored by configurator.
  * All members are declared as "def" since not all subclasses intend to represent all members in restful APIs.
  */
trait Data {
  def name: String
  def lastModified: Long
  def kind: String
  def tags: Map[String, JsValue]
}

object Data {

  /**
    * the default group to all objects.
    * the group is useful to Ohara Manager. However, in simple case, the group is a bit noisy so we offer the default group to all objects when
    * input group is ignored.
    */
  val DEFAULT_GROUP = "default"
  val NAME_KEY = "name"
  val TAGS_KEY = "tags"
}
