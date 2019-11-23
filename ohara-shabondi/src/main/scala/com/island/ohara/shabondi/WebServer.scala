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

package com.island.ohara.shabondi

import java.util.concurrent.atomic.AtomicReference

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.settings.ServerSettings
import akka.stream.ActorMaterializer
import com.island.ohara.common.util.CommonUtils

import scala.concurrent.duration.Duration
import scala.concurrent._
import scala.io.StdIn
import scala.util.{Failure, Success, Try}

private class WebServer(config: Config) extends AbstractWebServer {
  import DefaultDefinitions._

  private implicit val _system = ActorSystem("shabondi")

  def start(): Unit = {
    start(CommonUtils.anyLocalAddress(), config.port, ServerSettings(_system), _system)
  }

  override def routes: Route = {
    implicit val materializer = ActorMaterializer()

    val sourceRoute = SourceRoute(config)
    val sinkRoute   = SinkRoute(config)

    config.serverType match {
      case SERVER_TYPE_SOURCE => sourceRoute.route(config.sourceToTopics)
      case SERVER_TYPE_SINK   => sinkRoute.route
      case t                  => throw new RuntimeException(s"Invalid server type: $t")
    }
  }
}

/**
  * reference: akka.http.scaladsl.server.HttpApp
  */
private abstract class AbstractWebServer extends Directives {
  protected val actorSystemRef = new AtomicReference[ActorSystem]()

  protected def routes: Route

  protected def postBinding(binding: ServerBinding): Unit = {
    val hostname = binding.localAddress.getHostName
    val port     = binding.localAddress.getPort
    actorSystemRef.get().log.info(s"Server online at http://$hostname:$port/")
  }

  protected def postBindingFailure(cause: Throwable): Unit = {
    actorSystemRef.get().log.error(cause, s"Error starting the server ${cause.getMessage}")
  }

  protected def waitForShutdownSignal(system: ActorSystem)(implicit ec: ExecutionContext): Future[Done] = {
    val promise = Promise[Done]()
    sys.addShutdownHook {
      promise.trySuccess(Done)
    }
    Future {
      blocking {
        if (StdIn.readLine("Press <RETURN> to stop Shabondi WebServer...\n") != null)
          promise.trySuccess(Done)
      }
    }
    promise.future
  }

  protected def postServerShutdown(attempt: Try[Done], system: ActorSystem): Unit = {
    actorSystemRef.get().log.info("Shutting down the server")
  }

  protected def start(host: String, port: Int, settings: ServerSettings, actorSystem: ActorSystem): Unit = {
    implicit val _actorSystem = actorSystem

    actorSystemRef.set(actorSystem)

    implicit val materializer: ActorMaterializer            = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

    val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(
      handler = routes,
      interface = host,
      port = port,
      settings = settings
    )

    bindingFuture.onComplete {
      case Success(binding) =>
        postBinding(binding)
      case Failure(cause) =>
        postBindingFailure(cause)
    }

    Await.ready(
      bindingFuture.flatMap(_ => waitForShutdownSignal(actorSystem)),
      Duration.Inf
    )

    bindingFuture
      .flatMap(_.unbind())
      .onComplete(attempt => {
        postServerShutdown(attempt, actorSystem)
        actorSystem.terminate()
      })
  }
}
