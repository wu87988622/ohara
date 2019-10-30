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

import com.island.ohara.client.configurator.v0.{QueryApi, WorkerApi}
import com.island.ohara.client.configurator.v0.QueryApi.{RdbColumn, RdbInfo}
import com.island.ohara.client.database.DatabaseClient
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator
import com.island.ohara.testing.service.Database
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestQueryRoute extends OharaTest with Matchers {
  private[this] val db = Database.local()
  private[this] val configurator = Configurator.builder.fake().build()

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  private[this] val workerClusterInfo = result(
    WorkerApi.access.hostname(configurator.hostname).port(configurator.port).list()).head

  @Test
  def testQueryDb(): Unit = {
    val tableName = CommonUtils.randomString(10)
    val dbClient = DatabaseClient.builder.url(db.url()).user(db.user()).password(db.password()).build
    try {
      val r = result(
        QueryApi.access
          .hostname(configurator.hostname)
          .port(configurator.port)
          .rdbRequest
          .jdbcUrl(db.url())
          .user(db.user())
          .password(db.password())
          .workerClusterKey(workerClusterInfo.key)
          .query())
      r.name shouldBe "mysql"
      r.tables.isEmpty shouldBe true

      val cf0 = RdbColumn("cf0", "INTEGER", true)
      val cf1 = RdbColumn("cf1", "INTEGER", false)
      def verify(info: RdbInfo): Unit = {
        info.tables.count(_.name == tableName) shouldBe 1
        val table = info.tables.filter(_.name == tableName).head
        table.columns.size shouldBe 2
        table.columns.count(_.name == cf0.name) shouldBe 1
        table.columns.filter(_.name == cf0.name).head.pk shouldBe cf0.pk
        table.columns.count(_.name == cf1.name) shouldBe 1
        table.columns.filter(_.name == cf1.name).head.pk shouldBe cf1.pk
      }
      dbClient.createTable(tableName, Seq(cf0, cf1))

      verify(
        result(
          QueryApi.access
            .hostname(configurator.hostname)
            .port(configurator.port)
            .rdbRequest
            .jdbcUrl(db.url())
            .user(db.user())
            .password(db.password())
            .workerClusterKey(workerClusterInfo.key)
            .query()))

      verify(
        result(
          QueryApi.access
            .hostname(configurator.hostname)
            .port(configurator.port)
            .rdbRequest
            .jdbcUrl(db.url())
            .user(db.user())
            .password(db.password())
            .catalogPattern(db.databaseName)
            .tableName(tableName)
            .workerClusterKey(workerClusterInfo.key)
            .query()))
      dbClient.dropTable(tableName)
    } finally dbClient.close()
  }

  @After
  def tearDown(): Unit = {
    Releasable.close(configurator)
    Releasable.close(db)
  }
}
