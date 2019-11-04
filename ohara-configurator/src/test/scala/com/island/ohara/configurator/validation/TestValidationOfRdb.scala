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

package com.island.ohara.configurator.validation

import com.island.ohara.client.configurator.v0.InspectApi.RdbColumn
import com.island.ohara.client.configurator.v0.ValidationApi
import com.island.ohara.client.database.DatabaseClient
import com.island.ohara.common.util.CommonUtils
import org.junit.Test

import scala.concurrent.ExecutionContext.Implicits.global

class TestValidationOfRdb extends WithConfigurator {
  private[this] val rdb = testUtil.dataBase
  private[this] def request =
    ValidationApi.access.hostname(configuratorHostname).port(configuratorPort).rdbRequest

  @Test
  def goodCase(): Unit = {
    val client = DatabaseClient.builder.url(rdb.url()).user(rdb.user()).password(rdb.password()).build
    try client.createTable("table", Seq(RdbColumn("v0", "integer", true)))
    finally client.close()
    assertJdbcSuccess(
      result(request.jdbcUrl(rdb.url).user(rdb.user).password(rdb.password).workerClusterKey(workerClusterKey).verify())
    )
  }

  @Test
  def badCase(): Unit = assertJdbcFailure(
    result(
      request
        .jdbcUrl(rdb.url)
        .user(rdb.user)
        .password(CommonUtils.randomString())
        .workerClusterKey(workerClusterKey)
        .verify())
  )
}
