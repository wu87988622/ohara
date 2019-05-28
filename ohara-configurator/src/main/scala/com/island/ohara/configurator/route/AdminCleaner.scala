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

package com.island.ohara.configurator.route

import java.util
import java.util.concurrent.{Executors, LinkedBlockingQueue, TimeUnit}
import java.util.concurrent.atomic.AtomicBoolean

import com.island.ohara.client.kafka.TopicAdmin
import com.island.ohara.common.util.{CommonUtils, Releasable}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Many routes need to access Kafka topic. However, the thread in route may be interrupted before closing topic admin.
  * Hence, we need a cleaner to avoid the resource leak.
  * @param timeout time to cleanup useless TopicAdmin
  */
class AdminCleaner(timeout: Duration) extends Releasable {
  private[this] val closed = new AtomicBoolean(false)
  private[this] val queue = new LinkedBlockingQueue[(Long, TopicAdmin)]
  private[route] val executor = {
    val exec = Executors.newSingleThreadExecutor()
    // close and remove timeout admin objects
    def cleanup(timeout: Duration): Unit = {
      val buf = new util.LinkedList[(Long, TopicAdmin)]()
      try while (!queue.isEmpty) {
        val obj = queue.take()
        if (timeout != null && CommonUtils.current() - obj._1 <= timeout.toMillis) buf.add(obj)
        else if (!obj._2.closed()) Releasable.close(obj._2)
      } finally queue.addAll(buf)
    }
    Future {
      try while (!closed.get()) {
        cleanup(timeout)
        TimeUnit.SECONDS.sleep(5)
      } finally cleanup(null)
    }(ExecutionContext.fromExecutor(exec))
    exec
  }

  def add(topicAdmin: TopicAdmin): TopicAdmin = if (closed.get())
    throw new IllegalArgumentException("cleaner is closed")
  else {
    queue.put((CommonUtils.current(), topicAdmin))
    topicAdmin
  }

  override def close(): Unit = {
    closed.set(true)
    executor.shutdown()
    executor.awaitTermination(30, TimeUnit.SECONDS)
  }
}
