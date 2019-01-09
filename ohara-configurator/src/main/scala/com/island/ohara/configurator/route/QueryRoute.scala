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
            try RdbInfo(client.name,
                        client.tables(query.catalogPattern.orNull, query.schemaPattern.orNull, query.tableName.orNull))
            finally client.close()
          complete(rdb)
        }
      }
    }
  }
}
