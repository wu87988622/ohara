package com.island.ohara.manager

import java.io.File

import akka.actor.ActorSystem
import akka.event.{Logging, LogSource}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default
import akka.stream.ActorMaterializer
import com.island.ohara.io.CloseOnce._
import com.island.ohara.manager.sample.UserRoutes
import com.island.ohara.rest.RestClient

import scala.concurrent.Await
import scala.io.StdIn

/**
  * TODO: this class need refactor...we should divide the http server to a separate class and run the class as main process.. by chia
  */
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

  val HELP_KEY = "--help"
  val HOSTNAME_KEY = "--hostname"
  val PORT_KEY = "--port"
  val CONFIGURATOR_KEY = "--configurator"
  val USAGE = s"[Usage] $HOSTNAME_KEY $PORT_KEY $CONFIGURATOR_KEY"

  def main(args: Array[String]): Unit = {
    if (args.length == 1 && args(0).equals(HELP_KEY)) {
      println(USAGE)
      return
    }
    if (args.size % 2 != 0) throw new IllegalArgumentException(USAGE)
    // TODO: make the parse more friendly
    var hostname: Option[String] = Some("localhost")
    var port: Option[Int] = Some(0)
    var configurator: Option[(String, Int)] = None
    args.sliding(2, 2).foreach {
      case Array(HOSTNAME_KEY, value)     => hostname = Some(value)
      case Array(PORT_KEY, value)         => port = Some(value.toInt)
      case Array(CONFIGURATOR_KEY, value) => configurator = Some((value.split(":")(0), value.split(":")(1).toInt))
      case _                              => throw new IllegalArgumentException(USAGE)
    }
    if (configurator.isEmpty) log.info("No running configurator!!!")

    implicit val materializer = ActorMaterializer()

    try {
      val webRoot: File = this.webRoot(PROP_WEBROOT_DEFAULT)
      log.info("Ohara-manager web root: " + webRoot.getCanonicalPath)

      // TODO: a temporary information of configurator. How we pass the configurator information to ohara manager?
      doClose(new ApiRoutes(system, RestClient("localhost", 9999, system))) { apiRoutes =>
        {
          val route =
            apiRoutes.routes ~
              pathPrefix(Segments) { names =>
                get {
                  getFromDirectory(new File(webRoot, names.mkString("/")).getCanonicalPath)
                }
              } ~
              userRoutes
          import scala.concurrent.duration._
          val server = Await.result(Http().bindAndHandle(route, "localhost", 8080), 10 seconds)
          try {
            log.info(
              s"Ohara-manager online at http://${server.localAddress.getHostName}:${server.localAddress.getPort}/main/index.html")

            //Await.result(system.whenTerminated, Duration.Inf)   // await infinite

            log.info("Press RETURN to stop...")
            StdIn.readLine()
          } finally server.unbind()
        }
      }
    } catch {
      case e: Throwable =>
        log.error(e, e.toString)
    } finally {
      system.terminate()
    }
  }

}
