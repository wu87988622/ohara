package com.island.ohara.configurator.call

import com.island.ohara.config.OharaConfig
import com.island.ohara.configurator.data.OharaData
import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestCallQueueData extends SmallTest with Matchers {

  private[this] def checkJsonContent(data: OharaData) = {
    data.properties.foreach(prop => data.toJson(true).asString.contains(prop.alias))
    data.properties.foreach(prop => data.toJson(false).asString.contains(prop.key))
  }

  @Test
  def testOharaRequest(): Unit = {
    val uuid = testName.getMethodName
    val lease: Long = 1000
    def assert(request: OharaRequest) = {
      request.uuid shouldBe uuid
      request.name shouldBe OharaRequest.getClass.getSimpleName
      request.lease shouldBe lease
      checkJsonContent(request)

      val uuid2 = "uuid2"
      val name2 = "name2"
      val lease2: Long = 5000
      request.copy(OharaData.uuidProperty, uuid2).uuid shouldBe uuid2
      request.copy(OharaData.nameProperty, name2).name shouldBe name2
      request.copy(OharaRequest.leaseProperty, lease2).lease shouldBe lease2
    }
    assert(OharaRequest(uuid, lease))

    val oharaConfig = OharaConfig()
    an[IllegalArgumentException] should be thrownBy new OharaRequest(oharaConfig)
    OharaData.uuidProperty.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaRequest(oharaConfig)
    OharaData.nameProperty.set(oharaConfig, OharaRequest.getClass.getSimpleName)
    OharaRequest.leaseProperty.set(oharaConfig, lease)
    assert(new OharaRequest(oharaConfig))
  }

  @Test
  def testOharaResponse(): Unit = {
    val uuid = testName.getMethodName
    val requestUuit = testName.getMethodName + "-REQ"
    def assert(response: OharaResponse) = {
      response.uuid shouldBe uuid
      response.name shouldBe OharaResponse.getClass.getSimpleName
      response.requestId shouldBe requestUuit
      checkJsonContent(response)

      val uuid2 = "uuid2"
      val name2 = "name2"
      val requestUuit2 = testName.getMethodName + "-REQ2"
      response.copy(OharaData.uuidProperty, uuid2).uuid shouldBe uuid2
      response.copy(OharaData.nameProperty, name2).name shouldBe name2
      response.copy(OharaResponse.requestIdProperty, requestUuit2).requestId shouldBe requestUuit2
    }
    assert(OharaResponse(uuid, requestUuit))

    val oharaConfig = OharaConfig()
    an[IllegalArgumentException] should be thrownBy new OharaResponse(oharaConfig)
    OharaData.uuidProperty.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaResponse(oharaConfig)
    OharaData.nameProperty.set(oharaConfig, OharaResponse.getClass.getSimpleName)
    an[IllegalArgumentException] should be thrownBy new OharaResponse(oharaConfig)
    OharaResponse.requestIdProperty.set(oharaConfig, requestUuit)
    assert(new OharaResponse(oharaConfig))
  }
}
