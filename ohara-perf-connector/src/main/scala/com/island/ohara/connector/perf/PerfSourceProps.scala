package com.island.ohara.connector.perf
import scala.concurrent.duration.Duration

case class PerfSourceProps(batch: Int, freq: Duration) {
  def toMap: Map[String, String] = Map(
    PERF_BATCH -> batch.toString,
    PERF_FREQUENCE -> freq.toString
  )
}

object PerfSourceProps {
  def apply(props: Map[String, String]): PerfSourceProps = PerfSourceProps(
    batch = props(PERF_BATCH).toInt,
    freq = Duration(props(PERF_FREQUENCE))
  )
}
