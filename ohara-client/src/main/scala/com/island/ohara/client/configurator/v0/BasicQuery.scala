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

import com.island.ohara.client.configurator.QueryRequest
import com.island.ohara.common.util.CommonUtils
import spray.json.{JsObject, JsValue}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
  * this basic query includes the parameters owned by all objects. Also, it host the query structure and it generate
  * the query request for the sub classes.
  * @tparam T result type
  */
trait BasicQuery[T] {
  private[this] val paras: mutable.Map[String, String] = mutable.Map[String, String]()

  def name(value: String): BasicQuery.this.type = set("name", value)

  def group(value: String): BasicQuery.this.type = set("group", value)

  def tags(tags: Map[String, JsValue]): BasicQuery.this.type = set("tags", JsObject(tags).toString())

  def lastModified(value: Long): BasicQuery.this.type = set("lastModified", value.toString)

  protected def set(key: String, value: String): BasicQuery.this.type = {
    paras += (CommonUtils.requireNonEmpty(key) -> CommonUtils.requireNonEmpty(value))
    this
  }

  /**
    * send the LIST request with query parameters.
    * @param executionContext thread pool
    * @return results
    */
  final def execute()(implicit executionContext: ExecutionContext): Future[Seq[T]] = doExecute(
    QueryRequest(paras.toMap))

  protected def doExecute(request: QueryRequest)(implicit executionContext: ExecutionContext): Future[Seq[T]]
}
