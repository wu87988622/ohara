package com.island.ohara.configurator.job

import com.island.ohara.config.OharaConfig
import com.island.ohara.configurator.data.OharaData
import com.island.ohara.rule.SmallTest
import com.island.ohara.serialization.{BOOLEAN, BYTES, DataType, INT}
import org.junit.Test
import org.scalatest.Matchers

class TestHttpJobData extends SmallTest with Matchers {

  @Test
  def testAction(): Unit = {
    // it should pass
    Action.all.foreach(action => Action.of(action.name))
  }

  @Test
  def testStatus(): Unit = {
    // it should pass
    Status.all.foreach(status => Status.of(status.name))
  }
  @Test
  def testHttpJobRequest(): Unit = {
    val uuid = "uuid"
    val name = "name"
    val action: Action = RUN
    val path = "path"
    val schema = Map("a" -> BYTES, "b" -> BOOLEAN)
    val config = Map("A" -> "b", "c" -> "d")
    def assert(request: HttpJobRequest) = {
      request.uuid shouldBe uuid
      request.name shouldBe name
      request.schema shouldBe schema
      request.config shouldBe config

      val action2: Action = PAUSE
      val schema2 = Map("a" -> BYTES, "b" -> BOOLEAN, "c" -> INT)
      val config2 = Map("A" -> "b", "c" -> "d", "AA" -> "CC")
      request.copy(HttpJobRequest.action, action2).action shouldBe action2
      request.copy[Map[String, DataType]](HttpJobRequest.schema, schema2).schema shouldBe schema2
      request.copy(HttpJobRequest.config, config2).config shouldBe config2
    }
    assert(HttpJobRequest(uuid, name, action, path, schema, config))

    val oharaConfig = OharaConfig()
    an[IllegalArgumentException] should be thrownBy new HttpJobRequest(oharaConfig)
    OharaData.uuid.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new HttpJobRequest(oharaConfig)
    OharaData.name.set(oharaConfig, name)
    an[IllegalArgumentException] should be thrownBy new HttpJobRequest(oharaConfig)
    HttpJobRequest.path.set(oharaConfig, path)
    an[IllegalArgumentException] should be thrownBy new HttpJobRequest(oharaConfig)
    HttpJobRequest.action.set(oharaConfig, action)
    an[IllegalArgumentException] should be thrownBy new HttpJobRequest(oharaConfig)
    HttpJobRequest.schema.set(oharaConfig, schema)
    an[IllegalArgumentException] should be thrownBy new HttpJobRequest(oharaConfig)
    HttpJobRequest.config.set(oharaConfig, config)
    assert(new HttpJobRequest(oharaConfig))

    HttpJobRequest(action, path, schema, config).name shouldBe classOf[HttpJobRequest].getSimpleName
  }
  @Test
  def testHttpJobResponse(): Unit = {
    val uuid = "uuid"
    val name = "name"
    val status = RUNNING
    val config = Map("A" -> "b", "c" -> "d")
    def assert(response: HttpJobResponse) = {
      response.uuid shouldBe uuid
      response.name shouldBe name
      response.status shouldBe status
      response.config shouldBe config

      val status2 = NON_RUNNING
      val config2 = Map("A" -> "b", "c" -> "d", "DDD" -> "22")
      response.copy(HttpJobResponse.status, status2).status shouldBe status2
      response.copy(HttpJobResponse.config, config2).config shouldBe config2
    }
    assert(HttpJobResponse(uuid, name, status, config))

    val oharaConfig = OharaConfig()
    an[IllegalArgumentException] should be thrownBy new HttpJobResponse(oharaConfig)
    OharaData.uuid.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new HttpJobRequest(oharaConfig)
    OharaData.name.set(oharaConfig, name)
    an[IllegalArgumentException] should be thrownBy new HttpJobRequest(oharaConfig)
    HttpJobResponse.status.set(oharaConfig, status)
    an[IllegalArgumentException] should be thrownBy new HttpJobResponse(oharaConfig)
    HttpJobResponse.config.set(oharaConfig, config)
    assert(new HttpJobResponse(oharaConfig))

    HttpJobResponse(status, config).name shouldBe classOf[HttpJobResponse].getSimpleName
  }
}
