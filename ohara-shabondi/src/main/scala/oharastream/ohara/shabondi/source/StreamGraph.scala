/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package oharastream.ohara.shabondi.source

import akka.Done
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import oharastream.ohara.common.data.Row
import oharastream.ohara.common.setting.TopicKey
import oharastream.ohara.kafka.Producer
import oharastream.ohara.shabondi.common.ConvertSupport

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

private[shabondi] object StreamGraph {
  import ConvertSupport._

  type ROWS = immutable.Iterable[Row]

  def fromSendRow(producer: Producer[Row, Array[Byte]], topicKeys: Seq[TopicKey], row: Row)(
    implicit executor: ExecutionContext
  ): RunnableGraph[Future[Done]] = {
    val source = Source.single(row)
    val flowSendRow = Flow[Row].mapAsync(4) { row =>
      Future.sequence(topicKeys.map { topicKey =>
        val sender = producer.sender().key(row).topicName(topicKey.topicNameOnKafka)
        sender.send.toScala
      })
    }
    val sink = Sink.ignore
    source.via(flowSendRow).toMat(sink)(Keep.right)
  }
}
