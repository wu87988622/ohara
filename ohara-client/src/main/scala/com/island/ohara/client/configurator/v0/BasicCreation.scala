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

import com.island.ohara.common.setting.ObjectKey
import spray.json.JsValue

/**
  * this is a basic interface of request to create a normal object resource.
  * We separate this interface with basic Data since request payload does not mean to be a "store-able" data
  */
trait BasicCreation {
  def key: ObjectKey = ObjectKey.of(group, name)

  def group: String

  /**
    * @return object name
    */
  def name: String

  def tags: Map[String, JsValue]
}
