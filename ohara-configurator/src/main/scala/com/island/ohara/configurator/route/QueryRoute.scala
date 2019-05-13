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
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.DatabaseClient
import com.island.ohara.client.configurator.v0.QueryApi._

/**
  * used to handle the "QUERY" APIs
  */
private[configurator] object QueryRoute extends SprayJsonSupport {

  def apply(): server.Route = pathPrefix(QUERY_PREFIX_PATH) {
    path(RDB_PREFIX_PATH) {
      post {
        entity(as[RdbQuery]) { query =>
          val client = DatabaseClient(query.url, query.user, query.password)
          val rdb =
            try RdbInfo(
              client.databaseType,
              client
                .tableQuery()
                .catalog(query.catalogPattern.orNull)
                .schema(query.schemaPattern.orNull)
                .tableName(query.tableName.orNull)
                .execute()
            )
            finally client.close()
          complete(rdb)
        }
      }
    }
  }
}
