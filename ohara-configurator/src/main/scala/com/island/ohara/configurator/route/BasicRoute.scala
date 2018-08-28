package com.island.ohara.configurator.route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.StandardRoute
import com.island.ohara.client.ConfiguratorJson.{Error, Pipeline, Status}
import com.island.ohara.configurator.Configurator.Store
import org.apache.commons.lang3.exception.ExceptionUtils

object BasicRoute extends SprayJsonSupport {
  def rejectNonexistentUuid(uuid: String): StandardRoute = complete(
    StatusCodes.BadRequest -> toResponse(new IllegalArgumentException(s"Failed to find a schema mapping to $uuid")))

  def assertNotRelated2Pipeline(uuid: String)(implicit store: Store): Unit =
    if (!store
          .data[Pipeline]
          .filter(pipeline =>
            pipeline.uuid.equals(uuid)
              || pipeline.rules.keys.toSet.contains(uuid)
              || pipeline.rules.values.toSet.contains(uuid))
          .isEmpty) throw new IllegalArgumentException(s"The uuid:${uuid} is used by pipeline")

  def assertNotRelated2RunningPipeline(uuid: String)(implicit store: Store): Unit =
    if (!store
          .data[Pipeline]
          .filter(_.status == Status.RUNNING)
          .filter(pipeline =>
            pipeline.uuid.equals(uuid)
              || pipeline.rules.keys.toSet.contains(uuid)
              || pipeline.rules.values.toSet.contains(uuid))
          .isEmpty) throw new IllegalArgumentException(s"The uuid:${uuid} is used by running pipeline")

  private[this] def toResponse(e: Throwable) =
    Error(e.getClass.getName, if (e.getMessage == null) "None" else e.getMessage, ExceptionUtils.getStackTrace(e))
}
