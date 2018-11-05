package com.island.ohara.configurator
import java.util.concurrent.Executors

import com.island.ohara.integration.{OharaTestUtil, With3Brokers3Workers}
import com.island.ohara.kafka.KafkaUtil
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class TestOhara773 extends With3Brokers3Workers with Matchers {

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
    OharaTestUtil.await(() => Configurator.hasRunningConfigurator, 60 seconds)
    Configurator.closeRunningConfigurator = true
    OharaTestUtil.await(() => !Configurator.hasRunningConfigurator, 60 seconds)

    KafkaUtil.exist(testUtil.brokersConnProps, topicName) shouldBe true

    Configurator.closeRunningConfigurator = false
    // second call - the topic $$methodName exists so no topic will be created
    Future[Unit] {
      // first call - the topic $methodName should be created
      Configurator.main(args)
    }
    OharaTestUtil.await(() => Configurator.hasRunningConfigurator, 60 seconds)
    Configurator.closeRunningConfigurator = true
    OharaTestUtil.await(() => !Configurator.hasRunningConfigurator, 60 seconds)
  }
}
