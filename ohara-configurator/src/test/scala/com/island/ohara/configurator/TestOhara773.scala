package com.island.ohara.configurator
import java.time.Duration
import java.util.concurrent.Executors

import com.island.ohara.common.util.CommonUtil
import com.island.ohara.integration.WithBrokerWorker
import com.island.ohara.kafka.KafkaUtil
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.{ExecutionContext, Future}

class TestOhara773 extends WithBrokerWorker with Matchers {

  @Test
  def shouldNotCreateTopicIfItExists(): Unit = {
    val topicName = methodName
    val args = Array(
      Configurator.BROKERS_KEY,
      testUtil.brokersConnProps,
      Configurator.WORKERS_KEY,
      testUtil.workersConnProps,
      Configurator.TOPIC_KEY,
      methodName
    )

    implicit val service = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
    Future[Unit] {
      // first call - the topic $methodName should be created
      Configurator.main(args)
    }
    CommonUtil.await(() => Configurator.hasRunningConfigurator, Duration.ofSeconds(60))
    Configurator.closeRunningConfigurator = true
    CommonUtil.await(() => !Configurator.hasRunningConfigurator, Duration.ofSeconds(60))

    KafkaUtil.exist(testUtil.brokersConnProps, topicName) shouldBe true

    Configurator.closeRunningConfigurator = false
    // second call - the topic $$methodName exists so no topic will be created
    Future[Unit] {
      // first call - the topic $methodName should be created
      Configurator.main(args)
    }
    CommonUtil.await(() => Configurator.hasRunningConfigurator, Duration.ofSeconds(60))
    Configurator.closeRunningConfigurator = true
    CommonUtil.await(() => !Configurator.hasRunningConfigurator, Duration.ofSeconds(60))
  }
}
