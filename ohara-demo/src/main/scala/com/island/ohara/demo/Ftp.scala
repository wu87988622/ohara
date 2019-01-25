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

import com.island.ohara.integration.FtpServer

object Ftp {
  private[this] val USER = "--user"
  private[this] val PASSWORD = "--password"
  private[this] val CONTROL_PORT = "--controlPort"
  private[this] val DATA_PORTS = "--dataPorts"
  private[this] val TTL = "--ttl"
  private[this] val USAGE = s"$USER $PASSWORD $CONTROL_PORT $DATA_PORTS(form: 12345,12346 or 12345-12350)"
  def main(args: Array[String]): Unit = {
    var user = "user"
    var password = "password"
    var controlPort: Option[Int] = None
    var dataPorts: Array[Int] = Array.empty
    var ttl = Int.MaxValue
    args.sliding(2, 2).foreach {
      case Array(USER, value)         => user = value
      case Array(PASSWORD, value)     => password = value
      case Array(CONTROL_PORT, value) => controlPort = Some(value.toInt)
      case Array(DATA_PORTS, value) =>
        dataPorts =
          if (value.contains("-")) (value.split("-").head.toInt until value.split("-").last.toInt).toArray
          else value.split(",").map(_.toInt)
      case Array(TTL, value) => ttl = value.toInt
      case _                 => throw new IllegalArgumentException(USAGE)
    }
    if (dataPorts.isEmpty) throw new IllegalArgumentException(s"$DATA_PORTS is required")
    if (controlPort.isEmpty) throw new IllegalArgumentException(s"$CONTROL_PORT is required")

    val ftp = FtpServer.local(user, password, controlPort.get, dataPorts)
    try {
      println(s"user:${ftp.user()} password:${ftp.password()} hostname:${ftp.hostname()} port:${ftp
        .port()} absolutePath:${ftp.absolutePath()}")
      TimeUnit.SECONDS.sleep(ttl)
    } finally ftp.close()
  }
}
