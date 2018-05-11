package com.island.ohara.manager

import java.io.File
import java.nio.file.{Files, Path, Paths}

import akka.event.{LogSource, Logging}
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.directives._
import ContentTypeResolver.Default
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import com.island.ohara.manager.sample.{UserRegistryActor, UserRoutes}

import scala.io.StdIn

object HttpServer extends UserRoutes {
  val ENV_WEBROOT = "ohara.manager.webRoot"

  implicit val system = ActorSystem("ohara-manager")
  val userRegistryActor: ActorRef =
    system.actorOf(UserRegistryActor.props, "userRegistryActor")
  implicit val executionContext = system.dispatcher
  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName
    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }

  override lazy val log = Logging(system, this)

  private def srcWebRoot: Option[Path] = {
    val srcPath = Paths.get(new File("src/web").getCanonicalPath)
    Some(srcPath).filter(Files.exists(_))
  }

  private def sysWebRoot: Either[String, Path] =
    sys.props(ENV_WEBROOT) match {
      case null => Left(s"$ENV_WEBROOT not exist")
      case value =>
        Right(Paths.get(value))
          .filterOrElse(Files.exists(_), s"$ENV_WEBROOT is `$value`. But this folder is not exist.")
    }

  private def webRoot: Either[String, Path] = sysWebRoot match {
    case Left(msg) => srcWebRoot.toRight(msg)
    case result    => result
  }

  def main(args: Array[String]): Unit = {
    implicit val materializer = ActorMaterializer()

    try {
      val _webRoot: File = webRoot match {
        case Left(msg)   => throw new RuntimeException("Error: " + msg)
        case Right(path) => path.toFile
      }
      log.info(s"Ohara-manager web root: " + _webRoot.toString)

      val route =
        pathPrefix(Segments) { names =>
          get {
            getFromDirectory(new File(_webRoot, names.mkString("/")).getCanonicalPath)
          }
        } ~ userRoutes

      val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

      log.info(s"Ohara-manager online at http://localhost:8080/")

      //Await.result(system.whenTerminated, Duration.Inf)   // await infinite

      log.info("Press RETURN to stop...")
      StdIn.readLine()
      bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
    } catch {
      case e: Throwable =>
        log.error(e, e.toString)
        system.terminate()
    }
  }

}
