package com.island.ohara.prometheus

import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, JsValue, RootJsonFormat}

/**
  * Prometheus json object
  */
object PrometheusJson {

  //Targets Object
  final case class Targets(status: String, data: Data)
  final case class Data(activeTargets: Seq[TargetSeq], droppedTargets: Seq[TargetSeq])

  abstract sealed class Health extends Serializable {
    // adding a field to display the name from enumeration avoid we break the compatibility when moving code...
    val name: String
  }

  object Health {
    case object UP extends Health {
      val name = "up"
    }

    case object Down extends Health {
      val name = "down"
    }

    case object UnKnown extends Health {
      val name = "unknown"
    }

    val all: Seq[Health] = Seq(
      UP,
      Down,
      UnKnown
    )
  }

  implicit val STATE_JSON_FORMAT: RootJsonFormat[Health] = new RootJsonFormat[Health] {
    override def write(obj: Health): JsValue = JsString(obj.name)
    override def read(json: JsValue): Health = Health.all
      .find(_.name == json.asInstanceOf[JsString].value)
      .getOrElse(throw new IllegalArgumentException(s"Unknown state name:${json.asInstanceOf[JsString].value}"))
  }

  final case class TargetSeq(discoveredLabels: DiscoveredLabels,
                             labels: Labels,
                             scrapeUrl: String,
                             lastError: String,
                             lastScrape: String,
                             health: Health)
  final case class Labels(instance: String, job: String)
  final case class DiscoveredLabels(__address__ : String, __metrics_path__ : String, __scheme__ : String, job: String)

  implicit val DISCOVEREDLABELS_FORMAT: RootJsonFormat[DiscoveredLabels] = jsonFormat4(DiscoveredLabels)
  implicit val LABELS_FORMAT: RootJsonFormat[Labels] = jsonFormat2(Labels)
  implicit val TARGETSEQ_FORMAT: RootJsonFormat[TargetSeq] = jsonFormat6(TargetSeq)
  implicit val DATA_FORMAT: RootJsonFormat[Data] = jsonFormat2(Data)
  implicit val TARGET_FORMAT: RootJsonFormat[Targets] = jsonFormat2(Targets)

  //Config Object
  final case class Config(status: String, data: ConfigData)
  final case class ConfigData(yaml: String)

  implicit val CONFIGDATA_FORMAT: RootJsonFormat[ConfigData] = jsonFormat1(ConfigData)
  implicit val CONFIG_FORMAT: RootJsonFormat[Config] = jsonFormat2(Config)

}
