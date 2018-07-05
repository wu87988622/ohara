package com.island.ohara

import java.util.concurrent.TimeUnit

import com.island.ohara.configurator.Configurator
import com.island.ohara.configurator.kafka.KafkaClient
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.CloseOnce._
import com.island.ohara.manager.HttpServer

/**
  * a helper class used to run whole ohara service including kafka brokers, configurator and manager.
  */
object runOhara {

  def main(args: Array[String]) = {
    doClose(OharaTestUtil.localBrokers(3)) { util =>
      {
        println("wait for the mini cluster")
        TimeUnit.SECONDS.sleep(5)
        doClose(
          Configurator.builder
            .kafkaClient(KafkaClient(util.brokersString))
            .hostname("localhost")
            .port(0)
            .store(Configurator.storeBuilder.topicName("demo-ohara").brokers(util.brokersString).build())
            .build()) { configurator =>
          {
            // TODO: we should make the manage be a component rather than a fat main process...
            HttpServer.main(
              Array[String](HttpServer.HOSTNAME_KEY,
                            "localhost",
                            HttpServer.PORT_KEY,
                            "0",
                            HttpServer.CONFIGURATOR_KEY,
                            s"${configurator.hostname}:${configurator.port}") ++ args)
          }
        }
      }
    }
  }

}
