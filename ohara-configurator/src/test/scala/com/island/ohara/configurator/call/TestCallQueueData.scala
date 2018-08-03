package com.island.ohara.configurator.call

import com.island.ohara.config.OharaConfig
import com.island.ohara.data.OharaData
import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestCallQueueData extends SmallTest with Matchers {

  private[this] def checkJsonContent(data: OharaData) = {
    data.properties.foreach(prop => data.toJson.toString.contains(prop.key))
  }

  @Test
  def testOharaRequest(): Unit = {
    val uuid = methodName
    val lease: Long = 1000
    def assert(request: OharaRequest) = {
      request.uuid shouldBe uuid
      request.name shouldBe OharaRequest.getClass.getSimpleName
      request.lease shouldBe lease
      checkJsonContent(request)

      val uuid2 = "uuid2"
      val name2 = "name2"
      val lease2: Long = 5000
      request.copy(OharaData.UUID, uuid2).uuid shouldBe uuid2
      request.copy(OharaData.NAME, name2).name shouldBe name2
      request.copy(OharaRequest.LEASE, lease2).lease shouldBe lease2
    }
    assert(OharaRequest(uuid, lease))

    val oharaConfig = OharaConfig()
    an[IllegalArgumentException] should be thrownBy new OharaRequest(oharaConfig)
    OharaData.UUID.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaRequest(oharaConfig)
    OharaData.NAME.set(oharaConfig, OharaRequest.getClass.getSimpleName)
    OharaRequest.LEASE.set(oharaConfig, lease)
    assert(new OharaRequest(oharaConfig))
  }

  @Test
  def testOharaResponse(): Unit = {
    val uuid = methodName
    val requestUuit = methodName + "-REQ"
    def assert(response: OharaResponse) = {
      response.uuid shouldBe uuid
      response.name shouldBe OharaResponse.getClass.getSimpleName
      response.requestId shouldBe requestUuit
      checkJsonContent(response)

      val uuid2 = "uuid2"
      val name2 = "name2"
      val requestUuit2 = methodName + "-REQ2"
      response.copy(OharaData.UUID, uuid2).uuid shouldBe uuid2
      response.copy(OharaData.NAME, name2).name shouldBe name2
      response.copy(OharaResponse.REQUEST_ID, requestUuit2).requestId shouldBe requestUuit2
    }
    assert(OharaResponse(uuid, requestUuit))

    val oharaConfig = OharaConfig()
    an[IllegalArgumentException] should be thrownBy new OharaResponse(oharaConfig)
    OharaData.UUID.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaResponse(oharaConfig)
    OharaData.NAME.set(oharaConfig, OharaResponse.getClass.getSimpleName)
    an[IllegalArgumentException] should be thrownBy new OharaResponse(oharaConfig)
    OharaResponse.REQUEST_ID.set(oharaConfig, requestUuit)
    assert(new OharaResponse(oharaConfig))
  }
}
