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

package com.island.ohara.configurator.route

import java.net.URL

import scala.concurrent.Future

/**
  * just a interface to bridge jar store and route
  */
trait UrlGenerator {

  /**
    * generate available url for the input id. Note that the input id must reference to a existent jar
    * @param id jar id
    * @return downloadable url
    */
  def url(id: String): Future[URL]
}
