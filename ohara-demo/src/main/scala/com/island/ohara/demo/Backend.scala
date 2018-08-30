package com.island.ohara.demo
import java.util.concurrent.TimeUnit

import com.island.ohara.configurator.Configurator
import com.island.ohara.configurator.store.Store
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.CloseOnce.doClose
import com.island.ohara.kafka.KafkaClient
import com.island.ohara.client.ConnectorClient
import com.island.ohara.serialization.Serializer
import scala.concurrent.duration._

/**
  * run a configurator based on 3 brokers and 3 workers.
  */
object Backend {
  val HELP_KEY = "--help"
  val PORT_KEY = "--port"
  val TTL_KEY = "--ttl"
  val USAGE = s"[Usage] $TTL_KEY $PORT_KEY"
  def main(args: Array[String]): Unit = {
    if (args.length == 1 && args(0).equals(HELP_KEY)) {
      println(USAGE)
      System.exit(1)
    }
    if (args.size % 2 != 0) {
      println(USAGE)
      System.exit(1)
    }
    // TODO: make the parse more friendly
    var ttl: Duration = 365 days
    var port: Int = 0
    args.sliding(2, 2).foreach {
      case Array(PORT_KEY, value) => port = value.toInt
      case Array(TTL_KEY, value)  => ttl = value.toInt seconds
      case _                      => throw new IllegalArgumentException(USAGE)
    }
    doClose(OharaTestUtil.localWorkers(3, 3)) { util =>
      println("wait for the mini kafka cluster")
      TimeUnit.SECONDS.sleep(5)
      println(s"Succeed to run the mini brokers: ${util.brokers} and workers:${util.workers}")
      val topicName = s"demo-${System.currentTimeMillis()}"
      val configurator = Configurator.builder
        .store(
          Store
            .builder(Serializer.STRING, Serializer.OBJECT)
            .brokers(util.brokers)
            .topicName(topicName)
            .numberOfReplications(1)
            .numberOfPartitions(1)
            .build())
        .kafkaClient(KafkaClient(util.brokers))
        .connectClient(ConnectorClient(util.workers))
        .hostname("0.0.0.0")
        .port(port)
        .build()
      try {
        val db = util.startLocalDataBase()
        println(s"Succeed to run a database url:${db.url} user:${db.user} password:${db.password}")
        println(s"run a configurator at ${configurator.hostname}:${configurator.port} with topic:$topicName")
        println(
          s"enter ctrl+c to terminate all processes (or all processes will be terminated after ${ttl.toSeconds} seconds")
        TimeUnit.SECONDS.sleep(ttl.toSeconds)
      } finally configurator.close()
    }
  }
}
