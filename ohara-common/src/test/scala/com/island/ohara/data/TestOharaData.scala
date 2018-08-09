package com.island.ohara.data

import com.island.ohara.config.{OharaConfig, OharaProperty}
import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestOharaData extends SmallTest with Matchers {

  @Test
  def testEquals(): Unit = {
    val config0 = OharaConfig()
    OharaData.UUID.set(config0, "123")
    OharaData.NAME.set(config0, "123")
    OharaData.IMPLEMENTATION_NAME.set(config0, "xxxx")
    val data0 = new OharaData(config0) {
      override def copy[T](prop: OharaProperty[T], value: T): OharaData = this
      override protected def extraProperties: Seq[OharaProperty[_]] = Seq.empty[OharaProperty[_]]
    }

    val config1 = OharaConfig()
    OharaData.UUID.set(config1, "1234")
    OharaData.NAME.set(config1, "123")
    OharaData.IMPLEMENTATION_NAME.set(config1, "xxxx")
    val data1 = new OharaData(config1) {
      override def copy[T](prop: OharaProperty[T], value: T): OharaData = this
      override protected def extraProperties: Seq[OharaProperty[_]] = Seq.empty[OharaProperty[_]]
    }

    data0.equals(data1) shouldBe false
    data0.equals(data1, false) shouldBe true
  }

  @Test
  def testJobStatus(): Unit = {
    // it should pass
    PipelineStatus.all.foreach(status => PipelineStatus.of(status.name))
    PipelineStatus.all.foreach(status => PipelineStatus.of(status.name.toLowerCase))
  }

  private[this] def checkJsonContent(data: OharaData) = {
    data.properties.foreach(prop => data.toJson.toString.contains(prop.key))
  }

  @Test
  def testOharaJob(): Unit = {
    val uuid = methodName
    val name = "name"
    val status = PipelineStatus.RUNNING
    val rules = Map("cmp0" -> Seq("cmp1", "cmp2"))
    def assert(job: OharaPipeline) = {
      job.uuid shouldBe uuid
      job.name shouldBe name
      job.status shouldBe status
      val actualRules = job.rules
      actualRules.size shouldBe rules.size
      rules.foreach {
        case (k, v) =>
          actualRules.get(k) match {
            case Some(actualValue) => v.sameElements(actualValue) shouldBe true
            case None              => throw new IllegalArgumentException(s"miss $k")
          }
      }
      rules.size shouldBe 1
      val iter = rules.get("cmp0").get.iterator
      iter.next shouldBe "cmp1"
      iter.next shouldBe "cmp2"
      checkJsonContent(job)

      val uuid2 = "uuid2"
      val name2 = "name2"
      val status2 = PipelineStatus.STOP
      val rules2 = Map("rules2" -> Seq("rules2", "rules2"))
      job.copy(OharaData.UUID, uuid2).uuid shouldBe uuid2
      job.copy(OharaData.NAME, name2).name shouldBe name2
      job.copy(OharaPipeline.STATUS, status2).status shouldBe status2
      job.copy(OharaPipeline.RULES, rules2).rules.sameElements(rules2) shouldBe true
    }
    assert(OharaPipeline(uuid, name, PipelineStatus.RUNNING, Map("cmp0" -> Array("cmp1", "cmp2"))))

    val oharaConfig = OharaConfig()
    an[IllegalArgumentException] should be thrownBy new OharaPipeline(oharaConfig)
    OharaData.UUID.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaPipeline(oharaConfig)
    OharaData.NAME.set(oharaConfig, name)
    an[IllegalArgumentException] should be thrownBy new OharaPipeline(oharaConfig)
    OharaPipeline.STATUS.set(oharaConfig, status)
    an[IllegalArgumentException] should be thrownBy new OharaPipeline(oharaConfig)
    OharaPipeline.RULES.set(oharaConfig, rules)
    assert(new OharaPipeline(oharaConfig))
  }

  @Test
  def testOharaTarget(): Unit = {
    val uuid = methodName
    val name = "name"
    val configs = Map("a" -> "b", "c" -> "d")
    def assert(target: OharaTarget) = {
      target.uuid shouldBe uuid
      target.name shouldBe name
      target.configs.sameElements(configs) shouldBe true
      checkJsonContent(target)
    }
    assert(OharaTarget(uuid, name, configs))

    val oharaConfig = OharaConfig()
    an[IllegalArgumentException] should be thrownBy new OharaTarget(oharaConfig)
    OharaData.UUID.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaTarget(oharaConfig)
    OharaData.NAME.set(oharaConfig, name)
    an[IllegalArgumentException] should be thrownBy new OharaTarget(oharaConfig)
    OharaTarget.CONFIG.set(oharaConfig, configs)
    assert(new OharaTarget(oharaConfig))
  }

  @Test
  def testOharaSource(): Unit = {
    val uuid = methodName
    val name = "name"
    val configs = Map("a" -> "b", "c" -> "d")
    def assert(source: OharaSource) = {
      source.uuid shouldBe uuid
      source.name shouldBe name
      source.configs.sameElements(configs) shouldBe true
      checkJsonContent(source)
    }
    assert(OharaSource(uuid, name, configs))

    val oharaConfig = OharaConfig()
    an[IllegalArgumentException] should be thrownBy new OharaSource(oharaConfig)
    OharaData.UUID.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaSource(oharaConfig)
    OharaData.NAME.set(oharaConfig, name)
    an[IllegalArgumentException] should be thrownBy new OharaSource(oharaConfig)
    OharaSource.CONFIG.set(oharaConfig, configs)
    assert(new OharaSource(oharaConfig))
  }

  @Test
  def testOharaStreaming(): Unit = {
    val uuid = methodName
    val name = "name"
    val schemaId = "scheam"
    val topicId = "topic"
    def assert(streaming: OharaStreaming) = {
      streaming.uuid shouldBe uuid
      streaming.name shouldBe name
      streaming.schemaId shouldBe schemaId
      streaming.topicId shouldBe topicId
      checkJsonContent(streaming)
    }
    assert(OharaStreaming(uuid, name, schemaId, topicId))

    val oharaConfig = OharaConfig()
    an[IllegalArgumentException] should be thrownBy new OharaStreaming(oharaConfig)
    OharaData.UUID.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaStreaming(oharaConfig)
    OharaData.NAME.set(oharaConfig, name)
    an[IllegalArgumentException] should be thrownBy new OharaStreaming(oharaConfig)
    OharaStreaming.SCHEMA_ID.set(oharaConfig, schemaId)
    an[IllegalArgumentException] should be thrownBy new OharaStreaming(oharaConfig)
    OharaStreaming.TOPIC_ID.set(oharaConfig, topicId)
    assert(OharaStreaming(oharaConfig))
  }
}
