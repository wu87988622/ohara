package com.island.ohara.configurator
import com.island.ohara.client.ConfiguratorJson
import com.island.ohara.common.data.{Column, DataType}
import com.island.ohara.common.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers

class TestOhara734 extends SmallTest with Matchers {

  @Test
  def testColumn(): Unit = {
    import spray.json._
    val request = ConfiguratorJson.COLUMN_JSON_FORMAT.read("""
                                                           |{
                                                           |  "name":"cf",
                                                           |  "dataType":"boolean",
                                                           |  "order":1
                                                           |}
                                                         """.stripMargin.parseJson)
    request.name shouldBe "cf"
    request.newName shouldBe "cf"
    request.dataType shouldBe DataType.BOOLEAN
    request.order shouldBe 1

    val request2 = ConfiguratorJson.COLUMN_JSON_FORMAT.read("""
                                                              |{
                                                              |  "name":"cf",
                                                              |  "newName":null,
                                                              |  "dataType":"boolean",
                                                              |  "order":1
                                                              |}
                                                           """.stripMargin.parseJson)
    request2 shouldBe request

    val request3 = ConfiguratorJson.COLUMN_JSON_FORMAT.read("""
                                                              |{
                                                              |  "name":"cf",
                                                              |  "newName":"cf",
                                                              |  "dataType":"boolean",
                                                              |  "order":1
                                                              |}
                                                            """.stripMargin.parseJson)
    request3 shouldBe request
  }

  @Test
  def testSourceRequest(): Unit = {
    import spray.json._
    val request =
      ConfiguratorJson.SOURCE_REQUEST_JSON_FORMAT.read("""
                                                            |{
                                                            |  "name":"perf",
                                                            |  "className":"com.island.ohara.connector.perf.PerfSource",
                                                            |  "topics":["59e9010c-fd9c-4a41-918a-dacc9b84aa2b"],
                                                            |  "numberOfTasks":1,
                                                            |  "configs":{
                                                            |    "perf.batch":"1",
                                                            |    "perf.frequence":"2 seconds"
                                                            |  },
                                                            |  "schema":[{
                                                            |    "name": "cf0",
                                                            |    "newName": "cf0",
                                                            |    "dataType": "integer",
                                                            |    "order": 1
                                                            |  },{
                                                            |    "name": "cf1",
                                                            |    "newName": "cf1",
                                                            |    "dataType": "byte array",
                                                            |    "order": 2
                                                            |  }]
                                                            |}
                                                          """.stripMargin.parseJson)
    request.name shouldBe "perf"
    request.className shouldBe "com.island.ohara.connector.perf.PerfSource"
    request.topics.head shouldBe "59e9010c-fd9c-4a41-918a-dacc9b84aa2b"
    request.numberOfTasks shouldBe 1
    request.configs("perf.batch") shouldBe "1"
    request.schema.size shouldBe 2
    request.schema.head shouldBe Column.of("cf0", "cf0", DataType.INT, 1)
    request.schema.last shouldBe Column.of("cf1", "cf1", DataType.BYTES, 2)

    val request2 =
      ConfiguratorJson.SOURCE_REQUEST_JSON_FORMAT.read("""
                                                         |{
                                                         |  "name":"perf",
                                                         |  "className":"com.island.ohara.connector.perf.PerfSource",
                                                         |  "topics":["59e9010c-fd9c-4a41-918a-dacc9b84aa2b"],
                                                         |  "numberOfTasks":1,
                                                         |  "configs":{
                                                         |    "perf.batch":"1",
                                                         |    "perf.frequence":"2 seconds"
                                                         |  },
                                                         |  "schema":[{
                                                         |    "name": "cf0",
                                                         |    "newName": "cf0",
                                                         |    "dataType": "int",
                                                         |    "order": 1
                                                         |  },{
                                                         |    "name": "cf1",
                                                         |    "newName": "cf1",
                                                         |    "dataType": "bytes",
                                                         |    "order": 2
                                                         |  }]
                                                         |}
                                                       """.stripMargin.parseJson)
    request2 shouldBe request
  }

  @Test
  def testSinkRequest(): Unit = {
    import spray.json._
    val request =
      ConfiguratorJson.SINK_REQUEST_JSON_FORMAT.read("""
                                                               |{
                                                               |  "name":"perf",
                                                               |  "className":"com.island.ohara.connector.perf.PerfSource",
                                                               |  "topics":["59e9010c-fd9c-4a41-918a-dacc9b84aa2b"],
                                                               |  "numberOfTasks":1,
                                                               |  "configs":{
                                                               |    "perf.batch":"1",
                                                               |    "perf.frequence":"2 seconds"
                                                               |  },
                                                               |  "schema":[{
                                                               |    "name": "cf0",
                                                               |    "newName": "cf0",
                                                               |    "dataType": "integer",
                                                               |    "order": 1
                                                               |  },{
                                                               |    "name": "cf1",
                                                               |    "newName": "cf1",
                                                               |    "dataType": "byte array",
                                                               |    "order": 2
                                                               |  }]
                                                               |}
                                                             """.stripMargin.parseJson)
    request.name shouldBe "perf"
    request.className shouldBe "com.island.ohara.connector.perf.PerfSource"
    request.topics.head shouldBe "59e9010c-fd9c-4a41-918a-dacc9b84aa2b"
    request.numberOfTasks shouldBe 1
    request.configs("perf.batch") shouldBe "1"
    request.schema.size shouldBe 2
    request.schema.head shouldBe Column.of("cf0", "cf0", DataType.INT, 1)
    request.schema.last shouldBe Column.of("cf1", "cf1", DataType.BYTES, 2)

    val request2 =
      ConfiguratorJson.SINK_REQUEST_JSON_FORMAT.read("""
                                                       |{
                                                       |  "name":"perf",
                                                       |  "className":"com.island.ohara.connector.perf.PerfSource",
                                                       |  "topics":["59e9010c-fd9c-4a41-918a-dacc9b84aa2b"],
                                                       |  "numberOfTasks":1,
                                                       |  "configs":{
                                                       |    "perf.batch":"1",
                                                       |    "perf.frequence":"2 seconds"
                                                       |  },
                                                       |  "schema":[{
                                                       |    "name": "cf0",
                                                       |    "newName": "cf0",
                                                       |    "dataType": "int",
                                                       |    "order": 1
                                                       |  },{
                                                       |    "name": "cf1",
                                                       |    "newName": "cf1",
                                                       |    "dataType": "bytes",
                                                       |    "order": 2
                                                       |  }]
                                                       |}
                                                     """.stripMargin.parseJson)
    request2 shouldBe request
  }
}
