package com.island.ohara.configurator.data

import com.island.ohara.config.OharaConfig
import com.island.ohara.rule.SmallTest
import com.island.ohara.serialization.{BYTES, INT}
import org.junit.Test
import org.scalatest.Matchers

class TestOharaData extends SmallTest with Matchers {

  @Test
  def testJobStatus(): Unit = {
    // it should pass
    JobStatus.all.foreach(status => JobStatus.of(status.name))
    JobStatus.all.foreach(status => JobStatus.of(status.name.toLowerCase))
  }

  private[this] def checkJsonContent(data: OharaData) = {
    data.properties.foreach(prop => data.toJson(true).asString.contains(prop.alias))
    data.properties.foreach(prop => data.toJson(false).asString.contains(prop.key))
  }

  @Test
  def testOharaJob(): Unit = {
    val uuid = testName.getMethodName
    val name = "name"
    val status = JobStatus.RUNNING
    val rules = Map("cmp0" -> Seq("cmp1", "cmp2"))
    def assert(job: OharaJob) = {
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
      val status2 = JobStatus.STOP
      val rules2 = Map("rules2" -> Seq("rules2", "rules2"))
      job.copy(OharaData.uuidProperty, uuid2).uuid shouldBe uuid2
      job.copy(OharaData.nameProperty, name2).name shouldBe name2
      job.copy(OharaJob.statusProperty, status2).status shouldBe status2
      job.copy(OharaJob.rulesProperty, rules2).rules.sameElements(rules2) shouldBe true
    }
    assert(OharaJob(uuid, name, JobStatus.RUNNING, Map("cmp0" -> Array("cmp1", "cmp2"))))

    val oharaConfig = OharaConfig()
    an[IllegalArgumentException] should be thrownBy new OharaJob(oharaConfig)
    OharaData.uuidProperty.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaJob(oharaConfig)
    OharaData.nameProperty.set(oharaConfig, name)
    an[IllegalArgumentException] should be thrownBy new OharaJob(oharaConfig)
    OharaJob.statusProperty.set(oharaConfig, status)
    an[IllegalArgumentException] should be thrownBy new OharaJob(oharaConfig)
    OharaJob.rulesProperty.set(oharaConfig, rules)
    assert(new OharaJob(oharaConfig))
  }

  @Test
  def testOharaTarget(): Unit = {
    val uuid = testName.getMethodName
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
    OharaData.uuidProperty.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaTarget(oharaConfig)
    OharaData.nameProperty.set(oharaConfig, name)
    an[IllegalArgumentException] should be thrownBy new OharaTarget(oharaConfig)
    OharaTarget.configProperty.set(oharaConfig, configs)
    assert(new OharaTarget(oharaConfig))
  }

  @Test
  def testOharaSource(): Unit = {
    val uuid = testName.getMethodName
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
    OharaData.uuidProperty.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaSource(oharaConfig)
    OharaData.nameProperty.set(oharaConfig, name)
    an[IllegalArgumentException] should be thrownBy new OharaSource(oharaConfig)
    OharaSource.configProperty.set(oharaConfig, configs)
    assert(new OharaSource(oharaConfig))
  }

  @Test
  def testOharaTopic(): Unit = {
    val uuid = testName.getMethodName
    val name = "name"
    val numberOfPartition = 5
    val numberOfReplication = 10
    def assert(topic: OharaTopic) = {
      topic.uuid shouldBe uuid
      topic.name shouldBe name
      topic.numberOfPartition shouldBe numberOfPartition
      topic.numberOfReplication shouldBe numberOfReplication
      checkJsonContent(topic)
    }
    assert(OharaTopic(uuid, name, numberOfPartition, numberOfReplication))

    val oharaConfig = OharaConfig()
    an[IllegalArgumentException] should be thrownBy new OharaTopic(oharaConfig)
    OharaData.uuidProperty.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaTopic(oharaConfig)
    OharaData.nameProperty.set(oharaConfig, name)
    oharaConfig.set(OharaTopic.partitionProperty.key, numberOfPartition)
    oharaConfig.set(OharaTopic.replicationProperty.key, numberOfReplication)
    assert(OharaTopic(oharaConfig))
  }

  @Test
  def testOharaSchema(): Unit = {
    val uuid = testName.getMethodName
    val name = "name"
    val columns = Map("column-0" -> BYTES, "column-1" -> INT)
    def assert(schema: OharaSchema) = {
      schema.uuid shouldBe uuid
      schema.name shouldBe name
      schema.columns.sameElements(columns) shouldBe true
      checkJsonContent(schema)
    }
    assert(OharaSchema(uuid, name, columns))

    val oharaConfig = OharaConfig()
    an[IllegalArgumentException] should be thrownBy new OharaSchema(oharaConfig)
    OharaData.uuidProperty.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaSchema(oharaConfig)
    OharaData.nameProperty.set(oharaConfig, name)
    an[IllegalArgumentException] should be thrownBy new OharaSchema(oharaConfig)
    OharaSchema.columnType.set(oharaConfig, columns)
    assert(OharaSchema(oharaConfig))
  }

  @Test
  def testOharaStreaming(): Unit = {
    val uuid = testName.getMethodName
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
    OharaData.uuidProperty.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaStreaming(oharaConfig)
    OharaData.nameProperty.set(oharaConfig, name)
    an[IllegalArgumentException] should be thrownBy new OharaStreaming(oharaConfig)
    OharaStreaming.schemaIdProperty.set(oharaConfig, schemaId)
    an[IllegalArgumentException] should be thrownBy new OharaStreaming(oharaConfig)
    OharaStreaming.topicIdProperty.set(oharaConfig, topicId)
    assert(OharaStreaming(oharaConfig))
  }
}
