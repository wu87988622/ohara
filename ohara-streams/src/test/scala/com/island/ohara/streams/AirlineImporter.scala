package com.island.ohara.streams

import java.net.{InetAddress, NetworkInterface}
import java.nio.file.{Path, Paths}
import java.util.Properties

import com.island.ohara.data.{Cell, Row}
import com.island.ohara.kafka.Producer
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source

object AirlineImporter {

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger(AirlineImporter.getClass.getName)

  val CLINET_BOOTSTRAP_SERVERS = "9092".split(",").map(localHostAddress() + ":" + _).mkString(",")

  val TOPIC_CARRIERS = "carriers"
  val TOPIC_PLANE = "plane"
  val TOPIC_AIRPORT = "airport"
  val TOPIC_FLIGHT = "flight"

  type SendLine = (String, String) => Unit

  def main(args: Array[String]): Unit = {

    val bootstrapServers = CLINET_BOOTSTRAP_SERVERS
    importData(bootstrapServers, false)

  }

  def importData(bootstrapServers: String, useOharaAPI: Boolean): Unit = {

    val kafkaProducer: KafkaProducer[String, String] = createKafkaProducer(bootstrapServers)
    val kafkaSendLine: SendLine = (line, topicName) => {
      val record = new ProducerRecord[String, String](topicName, line)
      kafkaProducer.send(record, new KafkaProducerCallback(line))
    }

    // Note: value is ohara's Row type
    val oharaProducer: Producer[Array[Byte], Row] = Producer.builder().brokers(bootstrapServers).build
    val oharaSendLine: SendLine = (line, topicName) => {
      val sender = oharaProducer.sender()
      val row = Row(Cell("cf0", line))
      sender
        .key(null)
        .value(row)
        .send(topicName, {
          case Right(_) =>
          case Left(e)  => logger.error(e.getMessage)
        })
    }

    val prefix = "src/test/data"
    val fileCarrier: Path = Paths.get(s"$prefix/carriers.csv")
    val filePlane: Path = Paths.get(s"$prefix/plane-data.csv")
    val fileAirport: Path = Paths.get(s"$prefix/airports.csv")
    val fileFlight2007: Path = Paths.get(s"$prefix/2007-small.csv")
    val fileFlight2008: Path = Paths.get(s"$prefix/2008-small.csv")

    try {
      val sendLine = if (!useOharaAPI) oharaSendLine else kafkaSendLine
      val futureImportCarrier: Future[Unit] = asyncImportFile(TOPIC_CARRIERS, fileCarrier, sendLine, 0)
      val futureImportAirport: Future[Unit] = asyncImportFile(TOPIC_AIRPORT, fileAirport, sendLine, 0)
      val futureImportPlane: Future[Unit] = asyncImportFile(TOPIC_PLANE, filePlane, sendLine, 0)
      val futureImportFlight2007: Future[Unit] = asyncImportFile(TOPIC_FLIGHT, fileFlight2007, sendLine, 0)
      val futureImportFlight2008: Future[Unit] = asyncImportFile(TOPIC_FLIGHT, fileFlight2008, sendLine, 0)

      val finalFuture: Future[Unit] = for {
        _ <- futureImportCarrier
        _ <- futureImportPlane
        _ <- futureImportAirport
        _ <- futureImportFlight2007
        _ <- futureImportFlight2008
      } yield {}

      Await.result(finalFuture, Duration.Inf) // wait until all futures finished

    } finally {
      oharaProducer.close()
      kafkaProducer.close()
    }

  }

  private def asyncImportFile(topic: String,
                              file: Path,
                              sendLine: SendLine,
                              sleepMills: Long = 0,
                              sleepNanos: Int = 0): Future[Unit] = Future {
    val lines = Source.fromFile(file.toUri).getLines().drop(1)
    lines.foreach { line =>
      sendLine(line, topic)
      if (sleepMills > 0 || (sleepNanos > 0))
        Thread.sleep(sleepMills)
    }
  }

  private def createKafkaProducer(bootstrapServers: String): KafkaProducer[String, String] = {
    val props: Properties = new Properties
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
    props.put(ProducerConfig.CLIENT_ID_CONFIG, "airlines-import")
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    new KafkaProducer(props)
  }

  // https://stackoverflow.com/a/24649380/3155650
  private def localHostAddress(): String = {
    import collection.JavaConverters._
    val networkInterfaces = NetworkInterface.getNetworkInterfaces.asScala.toSeq

    val ipAddresses: Seq[InetAddress] = networkInterfaces.flatMap(p => p.getInetAddresses.asScala.toSeq)

    val address = ipAddresses
      .find { address =>
        val host = address.getHostAddress
        host.contains(".") && !address.isLoopbackAddress
      }
      .getOrElse(InetAddress.getLocalHost)

    address.getHostAddress
  }

  private class KafkaProducerCallback(val value: String) extends org.apache.kafka.clients.producer.Callback {
    val logger = Logger(classOf[KafkaProducerCallback])
    override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
      if (exception != null) {
        logger.error("Error: " + exception.getMessage)
      } else {
        //logger.info(value)
      }
    }
  }

  private def buildProperties(f: Properties => Unit): Properties = {
    val props: Properties = new Properties
    f.apply(props)
    props
  }
}
