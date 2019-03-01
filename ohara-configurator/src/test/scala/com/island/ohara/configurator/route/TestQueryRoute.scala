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

import com.island.ohara.client.DatabaseClient
import com.island.ohara.client.configurator.v0.QueryApi
import com.island.ohara.client.configurator.v0.QueryApi.{RdbColumn, RdbInfo, RdbQuery}
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.Releasable
import com.island.ohara.configurator.Configurator
import com.island.ohara.testing.service.Database
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestQueryRoute extends SmallTest with Matchers {
  private[this] val db = Database.local()
  private[this] val configurator = Configurator.builder().fake().build()

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def testQueryDb(): Unit = {
    val tableName = methodName
    val dbClient = DatabaseClient(db.url, db.user, db.password)
    try {
      val r = result(
        QueryApi
          .access()
          .hostname(configurator.hostname)
          .port(configurator.port)
          .query(RdbQuery(db.url, db.user, db.password, None, None, None)))
      r.name shouldBe "mysql"
      r.tables.isEmpty shouldBe true

      val cf0 = RdbColumn("cf0", "INTEGER", true)
      val cf1 = RdbColumn("cf1", "INTEGER", false)
      def verify(info: RdbInfo): Unit = {
        info.tables.count(_.name == tableName) shouldBe 1
        val table = info.tables.filter(_.name == tableName).head
        table.schema.size shouldBe 2
        table.schema.count(_.name == cf0.name) shouldBe 1
        table.schema.filter(_.name == cf0.name).head.pk shouldBe cf0.pk
        table.schema.count(_.name == cf1.name) shouldBe 1
        table.schema.filter(_.name == cf1.name).head.pk shouldBe cf1.pk
      }
      dbClient.createTable(tableName, Seq(cf0, cf1))

      verify(
        result(
          QueryApi
            .access()
            .hostname(configurator.hostname)
            .port(configurator.port)
            .query(RdbQuery(db.url, db.user, db.password, None, None, None))))
      verify(
        result(
          QueryApi
            .access()
            .hostname(configurator.hostname)
            .port(configurator.port)
            .query(RdbQuery(db.url, db.user, db.password, Some(db.databaseName), None, Some(tableName)))))
      dbClient.dropTable(tableName)
    } finally dbClient.close()
  }

  @After
  def tearDown(): Unit = {
    Releasable.close(configurator)
    Releasable.close(db)
  }
}
