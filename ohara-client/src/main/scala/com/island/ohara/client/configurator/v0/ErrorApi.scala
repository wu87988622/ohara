package com.island.ohara.client.configurator.v0
import org.apache.commons.lang3.exception.ExceptionUtils
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

object ErrorApi {

  /**
    * Although we have different version of APIs, the error response should be identical so as to simplify the parser...
    * @param code description of code
    * @param message error message
    * @param stack error stack
    */
  final case class Error(code: String, message: String, stack: String)

  def of(e: Throwable): Error =
    Error(e.getClass.getName, if (e.getMessage == null) "unknown" else e.getMessage, ExceptionUtils.getStackTrace(e))

  implicit val ERROR_FORMAT: RootJsonFormat[Error] = jsonFormat3(Error)
}
