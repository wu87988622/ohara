package com.island.ohara.configurator

import com.island.ohara.config.OharaJson
import com.island.ohara.configurator.data._
import com.island.ohara.configurator.store.MemStore
import com.island.ohara.io.CloseOnce._
import com.island.ohara.rest.RestClient
import com.island.ohara.rule.MediumTest
import com.island.ohara.serialization.{BYTES, INT, LONG, StringSerializer}
import org.junit.Test
import org.scalatest.Matchers

class TestConfigurator extends MediumTest with Matchers {

  @Test
  def testSchema(): Unit = {
    val store = new MemStore[String, OharaData](StringSerializer, OharaDataSerializer)
    val schemaName = "testSchema"
    val schemaType = Map("cf1" -> BYTES, "cf2" -> INT)
    val schemaIndex = Map("cf1" -> 1, "cf2" -> 2)
    val schema = OharaSchema.json(schemaName, schemaType, schemaIndex)
    val uuid = System.currentTimeMillis().toString
    val path = s"${Configurator.VERSION}/${Configurator.SCHEMA_PATH}"
    doClose(Configurator.builder.uuidGenerator(() => uuid).hostname("localhost").port(0).store(store).build()) {
      configurator =>
        {
          doClose(RestClient()) { client =>
            {
              var response = client.post(configurator.hostname, configurator.port, path, schema)
              response.statusCode shouldBe 200
              response.body shouldBe uuid
              configurator.schemas.size shouldBe 1
              configurator.schemas.next().name shouldBe schemaName
              configurator.schemas.next().types.foreach {
                case (name, t) => schemaType.get(name).get shouldBe t
              }

              response = client.get(configurator.hostname, configurator.port, s"$path/$uuid")
              response.statusCode shouldBe 200
              var returnedSchema = OharaSchema(OharaJson(response.body))
              configurator.schemas.next().types.foreach {
                case (name, t) => returnedSchema.types.get(name).get shouldBe t
              }

              response = client.delete(configurator.hostname, configurator.port, s"$path/phantom")
              response.statusCode shouldBe 400

              val newName = "testSchema"
              val newTypes = Map("cf3" -> BYTES, "cf2" -> LONG)
              val newIndexes = Map("cf3" -> 1, "cf2" -> 2)
              val newSchema = OharaSchema.json(newName, newTypes, newIndexes)
              response = client.put(configurator.hostname, configurator.port, s"$path/$uuid", newSchema)
              response.statusCode shouldBe 200

              response = client.get(configurator.hostname, configurator.port, s"$path/$uuid")
              response.statusCode shouldBe 200
              returnedSchema = OharaSchema(OharaJson(response.body))
              configurator.schemas.next().types.foreach {
                case (name, t) => returnedSchema.types.get(name).get shouldBe t
              }

              response = client.delete(configurator.hostname, configurator.port, s"$path/$uuid")
              response.statusCode shouldBe 200

              // we can't update a nonexistant schema
              response = client.put(configurator.hostname, configurator.port, s"$path/$uuid", newSchema)
              response.statusCode shouldBe 400

              response = client.get(configurator.hostname, configurator.port, s"$path/$uuid")
              response.statusCode shouldBe 400

              configurator.schemas.size shouldBe 0

              // add other ohara data
              val anotherUuid = (System.currentTimeMillis() + 100).toString
              val anotherName = "invalidschema"
              store.update(anotherUuid, OharaTopic.apply(anotherUuid, anotherName, 1, 1))
              response = client.get(configurator.hostname, configurator.port, s"$path/$anotherUuid")
              response.statusCode shouldBe 400
            }
          }
        }
    }
  }

  @Test
  def testSchemaIterator(): Unit = {
    val schemaCount = 100
    val schemas = (0 until schemaCount).map { index =>
      {
        OharaSchema.json(index.toString, Map(index.toString -> BYTES), Map(index.toString -> index))
      }
    }
    val path = s"${Configurator.VERSION}/${Configurator.SCHEMA_PATH}"
    val store = new MemStore[String, OharaData](StringSerializer, OharaDataSerializer)
    doClose(Configurator.builder.hostname("localhost").port(0).store(store).build()) { configurator =>
      {
        doClose(RestClient()) { client =>
          schemas.foreach(client.post(configurator.hostname, configurator.port, path, _).statusCode shouldBe 200)
        }
        configurator.schemas.size shouldBe schemaCount
      }
    }
  }

  @Test
  def testListSchemaUuid(): Unit = {
    val schemaCount = 10
    val uuids: Seq[String] = (0 until schemaCount).map(_.toString)
    var uuidIndex = 0
    val schemas: Seq[OharaJson] =
      (0 until schemaCount).map(index => OharaSchema.json(index.toString, Map("cf" -> BYTES), Map("cf" -> 1)))
    val store = new MemStore[String, OharaData](StringSerializer, OharaDataSerializer)
    val path = s"${Configurator.VERSION}/${Configurator.SCHEMA_PATH}"
    doClose(
      Configurator.builder
        .uuidGenerator(() => {
          uuidIndex <= uuids.size shouldBe true
          try uuids(uuidIndex)
          finally uuidIndex += 1
        })
        .hostname("localhost")
        .port(0)
        .store(store)
        .build()) { configurator =>
      {
        doClose(RestClient()) { client =>
          {
            schemas.zipWithIndex.foreach {
              case (schema, index) => {
                val response = client.post(configurator.hostname, configurator.port, path, schema)
                response.statusCode shouldBe 200
                response.body shouldBe uuids(index)
              }
            }
            // test list
            val response = client.get(configurator.hostname, configurator.port, path)
            response.statusCode shouldBe 200
            val responsedUuids = response.body.substring(1, response.body.size - 1).replace(" ", "").split(",")
            responsedUuids.size shouldBe uuids.size
            withClue(s"expected:${uuids.mkString(",")} actual:${responsedUuids.mkString(",")}")(
              responsedUuids.sameElements(uuids) shouldBe true)
          }
        }
      }
    }
  }

  @Test
  def testInvalidSchema(): Unit = {
    val store = new MemStore[String, OharaData](StringSerializer, OharaDataSerializer)
    val path = s"${Configurator.VERSION}/${Configurator.SCHEMA_PATH}"
    doClose(Configurator.builder.hostname("localhost").port(0).store(store).build()) { configurator =>
      {
        doClose(RestClient()) { client =>
          {
            var response = client.post(configurator.hostname, configurator.port, path, OharaJson("xxx"))
            response.statusCode shouldBe 400
            var exception = OharaException(OharaJson(response.body))
            exception.typeName.contains(classOf[IllegalArgumentException].getSimpleName)

            response = client.put(configurator.hostname, configurator.port, s"$path/12345", OharaJson("xxx"))
            response.statusCode shouldBe 400
            exception = OharaException(OharaJson(response.body))
            exception.typeName.contains(classOf[IllegalArgumentException].getSimpleName)
          }
        }
      }
    }
  }
}
