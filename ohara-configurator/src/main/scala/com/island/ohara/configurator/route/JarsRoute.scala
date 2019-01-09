package com.island.ohara.configurator.route

import java.io.File

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.FileInfo
import com.island.ohara.agent.jar.JarStore
import com.island.ohara.client.configurator.v0.JarApi._
import spray.json.DefaultJsonProtocol._
private[configurator] object JarsRoute {

  def tempDestination(fileInfo: FileInfo): File =
    File.createTempFile(fileInfo.fileName, ".tmp")

  def apply(implicit jarStore: JarStore): server.Route = pathPrefix(JAR_PREFIX_PATH) {
    storeUploadedFile("jar", tempDestination) {
      case (metadata, file) =>
        onSuccess(jarStore.add(file, metadata.fileName))(complete(_))
    } ~ get(onSuccess(jarStore.jarInfos())(complete(_))) ~ path(Segment) { id =>
      delete(onSuccess(jarStore.remove(id))(complete(_))) ~ get(onSuccess(jarStore.jarInfo(id))(complete(_)))
    }
  }
}
