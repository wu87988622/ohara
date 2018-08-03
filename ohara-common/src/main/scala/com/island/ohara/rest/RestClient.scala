package com.island.ohara.rest

import akka.actor.ActorSystem
import com.island.ohara.config.OharaJson
import com.island.ohara.io.CloseOnce

import scala.concurrent.duration.{Duration, _}

/**
  * A http client used to send the GET/PUT/DELETE/POST request to the target server.
  * The hostname and port should be in the configuration so the request method can take only path to compose the full http path.
  *
  * NOTED: it is not thread-safe.
  */
trait RestClient extends CloseOnce {

  /**
    * send a GET request to target server
    * @param path the resource path
    * @return (response code, response body)
    */
  def get(host: String, port: Int, path: String, timeout: Duration = RestClient.DEFAULT_REQUEST_TIMEOUT): RestResponse

  /**
    * send a DELETE request to target server
    * @param path the resource path
    * @return (response code, response body)
    */
  def delete(host: String,
             port: Int,
             path: String,
             timeout: Duration = RestClient.DEFAULT_REQUEST_TIMEOUT): RestResponse

  /**
    * send a PUT request to target server
    * @param path the resource path
    * @param body request payload in json format
    * @return (response code, response body)
    */
  def put(host: String,
          port: Int,
          path: String,
          body: OharaJson,
          timeout: Duration = RestClient.DEFAULT_REQUEST_TIMEOUT): RestResponse

  /**
    * send a POST request to target server
    * @param path the resource path
    * @param body request payload in json format
    * @return (response code, response body)
    */
  def post(host: String,
           port: Int,
           path: String,
           body: OharaJson,
           timeout: Duration = RestClient.DEFAULT_REQUEST_TIMEOUT): RestResponse
}

object RestClient {

  /**
    * Create a default impl of rest client.
    * @return a new RestClient
    */
  def apply(): RestClient = new AkkaRestClient()

  /**
    * Create a akka-based rest client sharing the ActorSystem
    * @param actorSystem shared actorSystem
    * @return a new RestClient
    */
  def apply(actorSystem: ActorSystem): RestClient = new AkkaRestClient(actorSystem)

  /**
    * the default timeout to wait the response from rest server.
    */
  val DEFAULT_REQUEST_TIMEOUT: Duration = 10 seconds

  val HTTP_SCHEME = "http"
}
