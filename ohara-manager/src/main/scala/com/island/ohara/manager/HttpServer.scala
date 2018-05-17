package com.island.ohara.manager

import java.io.File

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{LogSource, Logging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default
import akka.stream.ActorMaterializer
import com.island.ohara.manager.sample.UserRoutes

import scala.io.StdIn

object HttpServer extends UserRoutes {

  /**
    * This relative path is used for testing. For example, you can create the folder src/web in testing and then HttpServer will load it even if
    * no env variable is configured. You should, however, assign the absolute path by env variable in the production.
    */
  val PROP_WEBROOT_DEFAULT = "ohara-manager/src/web"
  val PROP_WEBROOT = "ohara.manager.webRoot"

  implicit val system = ActorSystem("ohara-manager")
  implicit val executionContext = system.dispatcher
  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName

    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }

  override lazy val log = Logging(system, this)

  /**
    * Try to locate the folder of web root. First, we lookup the env variable if user have configured a extra folder.
    * Second, we use the passed local folder if user doesn't config the env variable. If it fails to locate the web root, a exception will be thrown.
    * NOTED: make it package-private for testing
    * @param local local path
    */
  private[manager] def webRoot(local: String): File = {
    Option(sys.props(PROP_WEBROOT)).map(new File(_)).filter(_.exists()).getOrElse(new File(local)) match {
      case f: File if f.exists() => f
      case _ =>
        throw new RuntimeException(s"Fail to find the web root. Please set the $PROP_WEBROOT or check the $local")
    }
  }

  def main(args: Array[String]): Unit = {
    implicit val materializer = ActorMaterializer()

    try {
      val webRoot: File = this.webRoot(PROP_WEBROOT_DEFAULT)

      log.info("Ohara-manager web root: " + webRoot.getCanonicalPath)

      val route =
        pathPrefix(Segments) { names =>
          get {
            getFromDirectory(new File(webRoot, names.mkString("/")).getCanonicalPath)
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
