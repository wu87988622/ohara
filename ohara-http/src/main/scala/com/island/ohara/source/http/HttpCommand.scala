package com.island.ohara.source.http

import com.typesafe.config.Config

object HttpCommand {

  /**
    * `config` should contains
    *
    * {{{
    * bootstrap.servers = [
    *   "192.168.1.1:9092",
    *   "192.168.1.1:9092",
    *   "192.168.1.1:9092"
    * ]
    * }}}
    *
    * for KafkaProducer
    *
    * @param config
    */
  case class START(config: Config)
  object STOP
}
