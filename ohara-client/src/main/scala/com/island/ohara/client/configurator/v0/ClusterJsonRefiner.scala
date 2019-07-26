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
import com.island.ohara.common.util.CommonUtils

trait ClusterJsonRefiner[CREATION <: ClusterCreationRequest, UPDATE <: ClusterUpdateRequest] {

  def creation: JsonRefiner[CREATION]

  def update: JsonRefiner[UPDATE]
}

/**
  * A basic cluster JSON refiner object for Create and Update request.
  */
object ClusterJsonRefiner {

  /**
    * docker does limit the length of name (< 64). Since we format container name with some part of prefix,
    * limit the name length to one-third of 64 chars should be suitable for most cases.
    */
  private[this] val LIMIT_OF_NAME_LENGTH: Int = 20

  /**
    * use basic check rules of creation request for json refiner.
    * 1) reject any empty string.
    * 2) nodeName cannot use "start" and "stop" keywords.
    * 3) nodeName cannot be empty array.
    * 4) imageName will use {defaultImage} if not defined.
    * 5) name must satisfy the regex [a-z0-9] and length <= 20
    * 6) name will use randomString if not defined.
    * 7) tags will use empty map if not defined.
    * @param defaultImage this cluster default images
    * @tparam T type of creation
    * @return json refiner object
    */
  def basicRulesOfCreation[T <: ClusterCreationRequest](defaultImage: String): JsonRefiner[T] = JsonRefiner[T]
    .rejectEmptyString()
    .arrayRestriction("nodeNames")
    // we use the same sub-path for "node" and "actions" urls:
    // xxx/cluster/{name}/{node}
    // xxx/cluster/{name}/[start|stop]
    // the "actions" keywords must be avoided in nodeNames parameter
    .rejectKeyword(START_COMMAND)
    .rejectKeyword(STOP_COMMAND)
    // the node names can't be empty
    .rejectEmpty()
    .toRefiner
    .nullToString("imageName", defaultImage)
    .stringRestriction(Data.NAME_KEY)
    .withNumber()
    .withLowerCase()
    .withLengthLimit(LIMIT_OF_NAME_LENGTH)
    .toRefiner
    .nullToString(Data.NAME_KEY, () => CommonUtils.randomString(10))
    .nullToEmptyObject(Data.TAGS_KEY)

  /**
    * use basic check rules of update request for json refiner.
    * 1) reject any empty string.
    * 2) nodeName cannot use "start" and "stop" keywords.
    * 3) nodeName cannot be empty array.
    * @tparam T type of update
    * @return json refiner object
    */
  def basicRulesOfUpdate[T <: ClusterUpdateRequest]: JsonRefiner[T] = JsonRefiner[T]
    .rejectEmptyString()
    .arrayRestriction("nodeNames")
    // we use the same sub-path for "node" and "actions" urls:
    // xxx/cluster/{name}/{node}
    // xxx/cluster/{name}/[start|stop]
    // the "actions" keywords must be avoided in nodeNames parameter
    .rejectKeyword(START_COMMAND)
    .rejectKeyword(STOP_COMMAND)
    // the node names can't be empty
    .rejectEmpty()
    .toRefiner
}
