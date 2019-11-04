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

import java.util.Objects

import com.island.ohara.client.configurator.v0.InspectApi._
import com.island.ohara.common.setting.{ObjectKey, TopicKey}
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

// TODO: remove this https://github.com/oharastream/ohara/issues/3164
object QueryApi {

  final class Access private[QueryApi] extends BasicAccess(QUERY_PREFIX_PATH) {
    def rdbRequest: RdbRequest = new RdbRequest {
      private[this] var jdbcUrl: String = _
      private[this] var user: String = _
      private[this] var password: String = _
      private[this] var workerClusterKey: ObjectKey = _
      private[this] var catalogPattern: String = _
      private[this] var schemaPattern: String = _
      private[this] var tableName: String = _

      override def jdbcUrl(jdbcUrl: String): RdbRequest = {
        this.jdbcUrl = CommonUtils.requireNonEmpty(jdbcUrl)
        this
      }

      override def workerClusterKey(workerClusterKey: ObjectKey): RdbRequest = {
        this.workerClusterKey = Objects.requireNonNull(workerClusterKey)
        this
      }

      override def user(user: String): RdbRequest = {
        this.user = CommonUtils.requireNonEmpty(user)
        this
      }

      override def password(password: String): RdbRequest = {
        this.password = CommonUtils.requireNonEmpty(password)
        this
      }

      override def catalogPattern(catalogPattern: String): RdbRequest = {
        this.catalogPattern = CommonUtils.requireNonEmpty(catalogPattern)
        this
      }

      override def schemaPattern(schemaPattern: String): RdbRequest = {
        this.schemaPattern = CommonUtils.requireNonEmpty(schemaPattern)
        this
      }

      override def tableName(tableName: String): RdbRequest = {
        this.tableName = CommonUtils.requireNonEmpty(tableName)
        this
      }

      override private[v0] def query: RdbQuery = RdbQuery(
        url = CommonUtils.requireNonEmpty(jdbcUrl),
        user = CommonUtils.requireNonEmpty(user),
        password = CommonUtils.requireNonEmpty(password),
        workerClusterKey = Objects.requireNonNull(workerClusterKey),
        catalogPattern = Option(catalogPattern).map(CommonUtils.requireNonEmpty),
        schemaPattern = Option(schemaPattern).map(CommonUtils.requireNonEmpty),
        tableName = Option(tableName).map(CommonUtils.requireNonEmpty)
      )

      override def query()(implicit executionContext: ExecutionContext): Future[RdbInfo] =
        exec.post[RdbQuery, RdbInfo, ErrorApi.Error](s"$url/$RDB_PREFIX_PATH", query)
    }

    def topicRequest: TopicRequest = new TopicRequest {
      private[this] var key: TopicKey = _
      private[this] var limit = TOPIC_LIMIT_DEFAULT
      private[this] var timeout = TOPIC_TIMEOUT_DEFAULT

      override def key(key: TopicKey): TopicRequest = {
        this.key = Objects.requireNonNull(key)
        this
      }

      override def limit(limit: Int): TopicRequest = {
        this.limit = CommonUtils.requirePositiveInt(limit)
        this
      }

      override def timeout(timeout: Duration): TopicRequest = {
        this.timeout = Objects.requireNonNull(timeout)
        this
      }

      override def query()(implicit executionContext: ExecutionContext): Future[TopicData] =
        exec.post[TopicData, ErrorApi.Error](
          urlBuilder
            .key(key)
            .prefix(TOPIC_PREFIX_PATH)
            .param(TOPIC_LIMIT_KEY, limit.toString)
            .param(TOPIC_TIMEOUT_KEY, timeout.toMillis.toString)
            .build())
    }

    def fileRequest: FileRequest = new FileRequest {
      private[this] var key: ObjectKey = _

      override def key(key: ObjectKey): FileRequest = {
        this.key = Objects.requireNonNull(key)
        this
      }

      override def query()(implicit executionContext: ExecutionContext): Future[FileContent] =
        exec.get[FileContent, ErrorApi.Error](s"$url/$FILE_PREFIX_PATH/${key.name()}?$GROUP_KEY=${key.group()}")
    }
  }

  def access: Access = new Access
}
