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

package com.island.ohara.demo

import java.util.concurrent.TimeUnit

import com.island.ohara.integration.Database

/**
  * This main class is used to run mysql locally. In testing we always need a database to test jdbc connector.
  * We can't stop to govern everything, but we all love use someone's code nevertheless.
  */
object MySql {
  private[this] val USER = "--user"
  private[this] val PASSWORD = "--password"
  private[this] val DB_NAME = "--dbName"
  private[this] val PORT = "--port"
  private[this] val TTL = "--ttl"
  private[this] val USAGE = s"$USER $PASSWORD $PORT $DB_NAME"
  def main(args: Array[String]): Unit = {
    var user = "user"
    var password = "password"
    var port: Option[Int] = None
    var dbName = "ohara"
    var ttl = Int.MaxValue
    args.sliding(2, 2).foreach {
      case Array(USER, value)     => user = value
      case Array(PASSWORD, value) => password = value
      case Array(PORT, value)     => port = Some(value.toInt)
      case Array(DB_NAME, value)  => dbName = value
      case Array(TTL, value)      => ttl = value.toInt
      case _                      => throw new IllegalArgumentException(USAGE)
    }
    if (port.isEmpty) throw new IllegalArgumentException(s"$PORT is required")

    val database = Database.local(user, password, dbName, port.get)
    try {
      println(s"jdbc url:${database.url()} user:${database.user()} password:${database.password()}")
      TimeUnit.SECONDS.sleep(ttl)
    } finally database.close()
  }
}
