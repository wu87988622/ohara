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
    data.properties.foreach(prop => data.toJson.toString.contains(prop.key))
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
      job.copy(OharaData.uuid, uuid2).uuid shouldBe uuid2
      job.copy(OharaData.name, name2).name shouldBe name2
      job.copy(OharaJob.status, status2).status shouldBe status2
      job.copy(OharaJob.rules, rules2).rules.sameElements(rules2) shouldBe true
    }
    assert(OharaJob(uuid, name, JobStatus.RUNNING, Map("cmp0" -> Array("cmp1", "cmp2"))))

    val oharaConfig = OharaConfig()
    an[IllegalArgumentException] should be thrownBy new OharaJob(oharaConfig)
    OharaData.uuid.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaJob(oharaConfig)
    OharaData.name.set(oharaConfig, name)
    an[IllegalArgumentException] should be thrownBy new OharaJob(oharaConfig)
    OharaJob.status.set(oharaConfig, status)
    an[IllegalArgumentException] should be thrownBy new OharaJob(oharaConfig)
    OharaJob.rules.set(oharaConfig, rules)
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
    OharaData.uuid.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaTarget(oharaConfig)
    OharaData.name.set(oharaConfig, name)
    an[IllegalArgumentException] should be thrownBy new OharaTarget(oharaConfig)
    OharaTarget.config.set(oharaConfig, configs)
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
    OharaData.uuid.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaSource(oharaConfig)
    OharaData.name.set(oharaConfig, name)
    an[IllegalArgumentException] should be thrownBy new OharaSource(oharaConfig)
    OharaSource.config.set(oharaConfig, configs)
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
    OharaData.uuid.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaTopic(oharaConfig)
    OharaData.name.set(oharaConfig, name)
    oharaConfig.set(OharaTopic.partitionNumber.key, numberOfPartition)
    oharaConfig.set(OharaTopic.replicationNumber.key, numberOfReplication)
    assert(OharaTopic(oharaConfig))
  }

  @Test
  def testOharaSchema(): Unit = {
    val uuid = testName.getMethodName
    val name = "name"
    val columns = Map("column-0" -> BYTES, "column-1" -> INT)
    val indexes = Map("column-0" -> 0, "column-1" -> 1)
    def assert(schema: OharaSchema) = {
      schema.uuid shouldBe uuid
      schema.name shouldBe name
      schema.types.sameElements(columns) shouldBe true
      schema.orders.sameElements(indexes) shouldBe true
      checkJsonContent(schema)
    }
    assert(OharaSchema(uuid, name, columns, indexes))

    val oharaConfig = OharaConfig()
    an[IllegalArgumentException] should be thrownBy new OharaSchema(oharaConfig)
    OharaData.uuid.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaSchema(oharaConfig)
    OharaData.name.set(oharaConfig, name)
    an[IllegalArgumentException] should be thrownBy new OharaSchema(oharaConfig)
    OharaSchema.columnType.set(oharaConfig, columns)
    OharaSchema.columnOrder.set(oharaConfig, indexes)
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
    OharaData.uuid.set(oharaConfig, uuid)
    an[IllegalArgumentException] should be thrownBy new OharaStreaming(oharaConfig)
    OharaData.name.set(oharaConfig, name)
    an[IllegalArgumentException] should be thrownBy new OharaStreaming(oharaConfig)
    OharaStreaming.schemaId.set(oharaConfig, schemaId)
    an[IllegalArgumentException] should be thrownBy new OharaStreaming(oharaConfig)
    OharaStreaming.topicId.set(oharaConfig, topicId)
    assert(OharaStreaming(oharaConfig))
  }
}
