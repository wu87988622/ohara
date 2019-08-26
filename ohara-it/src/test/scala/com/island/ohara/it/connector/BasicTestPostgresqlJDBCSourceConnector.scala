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

package com.island.ohara.it.connector

import com.island.ohara.client.database.DatabaseClient

abstract class BasicTestPostgresqlJDBCSourceConnector extends BasicTestJDBCSourceConnector {

  override protected def jdbcJarUrl(): String =
    "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.2.6/postgresql-42.2.6.jar"

  override protected def dbUrl(): Option[String] = sys.env.get(DB_URL_KEY)

  override protected def dbUserName(): Option[String] = sys.env.get(DB_USER_NAME_KEY)

  override protected def dbPassword(): Option[String] = sys.env.get(DB_PASSWORD_KEY)

  override protected def dataBaseClient(): DatabaseClient =
    DatabaseClient.builder.url(dbUrl.get).user(dbUserName.get).password(dbPassword.get).build
}
