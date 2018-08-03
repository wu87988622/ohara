package com.island.ohara.rest
import akka.actor.ActorSystem
import com.island.ohara.config.OharaJson
import com.island.ohara.io.CloseOnce

import scala.concurrent.duration.Duration

/**
  * It is similar to RestClient but the target address and port is bound. Hence, all http method won't require the
  * hostname and port again.
  */
trait BoundRestClient extends CloseOnce {

  val hostname: String

  val port: Int

  /**
    * send a GET request to target server
    * @param path the resource path
    * @return (response code, response body)
    */
  def get(path: String, timeout: Duration = RestClient.DEFAULT_REQUEST_TIMEOUT): RestResponse

  /**
    * send a DELETE request to target server
    * @param path the resource path
    * @return (response code, response body)
    */
  def delete(path: String, timeout: Duration = RestClient.DEFAULT_REQUEST_TIMEOUT): RestResponse

  /**
    * send a PUT request to target server
    * @param path the resource path
    * @param body request payload in json format
    * @return (response code, response body)
    */
  def put(path: String, body: OharaJson, timeout: Duration = RestClient.DEFAULT_REQUEST_TIMEOUT): RestResponse

  /**
    * send a POST request to target server
    * @param path the resource path
    * @param body request payload in json format
    * @return (response code, response body)
    */
  def post(path: String, body: OharaJson, timeout: Duration = RestClient.DEFAULT_REQUEST_TIMEOUT): RestResponse
}

object BoundRestClient {

  /**
    * Create a default impl of rest client.
    * @param host the target address
    * @param port the target port
    * @return a new RestClient
    */
  def apply(host: String, port: Int): BoundRestClient = apply(host, port, null)

  /**
    * Create a default impl of rest client.
    * @param host the target address
    * @param _port the target port
    * @param actorSystem shared actorSystem.
    * @return a new RestClient
    */
  def apply(host: String, _port: Int, actorSystem: ActorSystem): BoundRestClient = new BoundRestClient() {
    private[this] val delegatee = RestClient(actorSystem)
    override def get(path: String, timeout: Duration): RestResponse = delegatee.get(hostname, port, path, timeout)

    override def delete(path: String, timeout: Duration): RestResponse = delegatee.delete(hostname, port, path, timeout)

    override def put(path: String, body: OharaJson, timeout: Duration): RestResponse =
      delegatee.put(hostname, port, path, body, timeout)

    override def post(path: String, body: OharaJson, timeout: Duration): RestResponse =
      delegatee.post(hostname, port, path, body, timeout)

    override def doClose(): Unit = delegatee.close()

    override val hostname: String = host
    override val port: Int = _port
  }
}
