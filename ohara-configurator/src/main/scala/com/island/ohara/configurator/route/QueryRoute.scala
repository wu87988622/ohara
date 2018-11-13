package com.island.ohara.configurator.route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.DatabaseClient
import com.island.ohara.client.util.CloseOnce._

/**
  * used to handle the "QUERY" APIs
  */
private[configurator] object QueryRoute extends SprayJsonSupport {

  def apply(): server.Route = pathPrefix(QUERY_PATH) {
    pathPrefix(RDB_PATH) {
      pathEnd {
        post {
          entity(as[RdbQuery]) { query =>
            val rdb = doClose(DatabaseClient(query.url, query.user, query.password)) { client =>
              RdbInformation(client.name,
                             client.tables(query.catalogPattern.orNull,
                                           query.schemaPattern.orNull,
                                           query.tableName.orNull))
            }
            complete(rdb)
          }
        }
      }
    }
  }
}
