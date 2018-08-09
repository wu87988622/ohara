package com.island.ohara.demo
import java.util.concurrent.TimeUnit

import com.island.ohara.configurator.Configurator
import com.island.ohara.configurator.store.Store
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.CloseOnce.doClose
import com.island.ohara.kafka.KafkaClient
import com.island.ohara.rest.ConnectorClient
import com.island.ohara.serialization.Serializer

/**
  * run a configurator based on 3 brokers and 3 workers.
  */
object Backend {
  def main(args: Array[String]): Unit = {
    doClose(OharaTestUtil.localWorkers(3, 3)) { util =>
      println("wait for the mini kafka cluster")
      TimeUnit.SECONDS.sleep(5)
      println(s"Succeed to run the mini brokers: ${util.brokersString} and workers:${util.workersString}")
      val topicName = s"demo-${System.currentTimeMillis()}"
      val configurator = Configurator.builder
        .store(
          Store
            .builder(Serializer.STRING, Serializer.OBJECT)
            .brokers(util.brokersString)
            .topicName(topicName)
            .numberOfReplications(1)
            .numberOfPartitions(1)
            .build())
        .kafkaClient(KafkaClient(util.brokersString))
        .connectClient(ConnectorClient(util.workersString))
        .hostname("localhost")
        .port(0)
        .build()
      try {
        println(s"run a configurator at ${configurator.hostname}:${configurator.port} with topic:$topicName")
        println(s"enter ctrl+c to terminate all process")
        TimeUnit.SECONDS.sleep(9999)
      } finally configurator.close()
    }
  }
}
