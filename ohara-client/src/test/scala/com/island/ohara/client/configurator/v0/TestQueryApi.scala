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

import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers

class TestQueryApi extends SmallTest with Matchers {

  @Test
  def testBasicQueryObject(): Unit = {
    val url = CommonUtils.randomString(10)
    val user = CommonUtils.randomString(10)
    val password = CommonUtils.randomString(10)
    val query = QueryApi.access
      .hostname(CommonUtils.randomString())
      .port(CommonUtils.availablePort())
      .request
      .url(url)
      .user(user)
      .password(password)
      .query

    query.url shouldBe url
    query.user shouldBe user
    query.password shouldBe password
    query.workerClusterName shouldBe None
    query.catalogPattern shouldBe None
    query.schemaPattern shouldBe None
    query.tableName shouldBe None
  }

  @Test
  def testQueryObjectWithAllFields(): Unit = {
    val url = CommonUtils.randomString(10)
    val user = CommonUtils.randomString(10)
    val password = CommonUtils.randomString(10)
    val workerClusterName = CommonUtils.randomString(10)
    val catalogPattern = CommonUtils.randomString(10)
    val schemaPattern = CommonUtils.randomString(10)
    val tableName = CommonUtils.randomString(10)
    val query = QueryApi.access
      .hostname(CommonUtils.randomString())
      .port(CommonUtils.availablePort())
      .request
      .url(url)
      .user(user)
      .password(password)
      .workerClusterName(workerClusterName)
      .catalogPattern(catalogPattern)
      .schemaPattern(schemaPattern)
      .tableName(tableName)
      .query

    query.url shouldBe url
    query.user shouldBe user
    query.password shouldBe password
    query.workerClusterName.get shouldBe workerClusterName
    query.catalogPattern.get shouldBe catalogPattern
    query.schemaPattern.get shouldBe schemaPattern
    query.tableName.get shouldBe tableName
  }

  @Test
  def ignoreUrlOnCreation(): Unit = an[NullPointerException] should be thrownBy QueryApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .user(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .query

  @Test
  def ignoreUserOnCreation(): Unit = an[NullPointerException] should be thrownBy QueryApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .url(CommonUtils.randomString())
    .password(CommonUtils.randomString())
    .query

  @Test
  def ignorePasswordOnCreation(): Unit = an[NullPointerException] should be thrownBy QueryApi.access
    .hostname(CommonUtils.randomString())
    .port(CommonUtils.availablePort())
    .request
    .url(CommonUtils.randomString())
    .user(CommonUtils.randomString())
    .query

  @Test
  def nullUrl(): Unit = an[NullPointerException] should be thrownBy QueryApi.access.request.url(null)

  @Test
  def emptyUrl(): Unit = an[IllegalArgumentException] should be thrownBy QueryApi.access.request.url("")

  @Test
  def nullUser(): Unit = an[NullPointerException] should be thrownBy QueryApi.access.request.user(null)

  @Test
  def emptyUser(): Unit = an[IllegalArgumentException] should be thrownBy QueryApi.access.request.user("")

  @Test
  def nullPassword(): Unit = an[NullPointerException] should be thrownBy QueryApi.access.request.password(null)

  @Test
  def emptyPassword(): Unit = an[IllegalArgumentException] should be thrownBy QueryApi.access.request.password("")

  @Test
  def nullWorkerClusterName(): Unit =
    an[NullPointerException] should be thrownBy QueryApi.access.request.workerClusterName(null)

  @Test
  def emptyWorkerClusterName(): Unit =
    an[IllegalArgumentException] should be thrownBy QueryApi.access.request.workerClusterName("")

  @Test
  def nullSchemaPattern(): Unit =
    an[NullPointerException] should be thrownBy QueryApi.access.request.schemaPattern(null)

  @Test
  def emptySchemaPattern(): Unit =
    an[IllegalArgumentException] should be thrownBy QueryApi.access.request.schemaPattern("")

  @Test
  def nullCatalogPattern(): Unit =
    an[NullPointerException] should be thrownBy QueryApi.access.request.catalogPattern(null)

  @Test
  def emptyCatalogPattern(): Unit =
    an[IllegalArgumentException] should be thrownBy QueryApi.access.request.catalogPattern("")

  @Test
  def nullTableName(): Unit = an[NullPointerException] should be thrownBy QueryApi.access.request.tableName(null)

  @Test
  def emptyTableName(): Unit = an[IllegalArgumentException] should be thrownBy QueryApi.access.request.tableName("")
}
