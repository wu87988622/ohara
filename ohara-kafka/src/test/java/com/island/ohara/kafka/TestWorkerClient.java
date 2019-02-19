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

package com.island.ohara.kafka;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import com.island.ohara.client.kafka.WorkerClient;
import com.island.ohara.client.kafka.WorkerClient$;
import com.island.ohara.common.data.Cell;
import com.island.ohara.common.data.ConnectorState;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Serializer;
import com.island.ohara.common.util.CommonUtil;
import com.island.ohara.integration.OharaTestUtil;
import com.island.ohara.integration.With3Brokers3Workers;
import com.island.ohara.kafka.Consumer.Record;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import scala.concurrent.Await;

// TODO: rewrite this stuff by scala ... by chia
public class TestWorkerClient extends With3Brokers3Workers {
  private final OharaTestUtil testUtil = testUtil();

  private final WorkerClient workerClient =
      WorkerClient$.MODULE$.apply(testUtil.workersConnProps(), 10);

  private static <T> T result(scala.concurrent.Future<T> f) {
    try {
      return Await.result(f, scala.concurrent.duration.Duration.create(10, TimeUnit.SECONDS));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean exist(String name) {
    try {
      return (Boolean) result(workerClient.exist(name));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testExist() throws Exception {
    String topicName = methodName();
    String connectorName = methodName();
    assertFalse(exist(connectorName));

    result(
        workerClient
            .connectorCreator()
            .topic(topicName)
            .connectorClass(MyConnector.class)
            .name(connectorName)
            .numberOfTasks(1)
            .disableConverter()
            .create());

    try {
      CommonUtil.await(() -> exist(connectorName), Duration.ofSeconds(50));
    } finally {
      workerClient.delete(connectorName);
    }
  }

  @Test
  public void testExistOnUnrunnableConnector() {
    String topicName = methodName();
    String connectorName = methodName();
    assertFalse(exist(connectorName));

    result(
        workerClient
            .connectorCreator()
            .topic(topicName)
            .connectorClass(UnrunnableConnector.class)
            .name(connectorName)
            .numberOfTasks(1)
            .disableConverter()
            .create());

    try {
      CommonUtil.await(() -> exist(connectorName), Duration.ofSeconds(50));
    } finally {
      workerClient.delete(connectorName);
    }
  }

  @Test
  public void testPauseAndResumeSource() {
    String topicName = methodName();
    String connectorName = methodName();
    workerClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(MyConnector.class)
        .name(connectorName)
        .numberOfTasks(1)
        .disableConverter()
        .create();
    CommonUtil.await(() -> exist(connectorName), Duration.ofSeconds(50));
    try (Consumer<Row, byte[]> consumer =
        Consumer.builder()
            .topicName(topicName)
            .offsetFromBegin()
            .connectionProps(testUtil.brokersConnProps())
            .build(Serializer.ROW, Serializer.BYTES)) {

      // try to receive some data from topic
      List<Record<Row, byte[]>> result = consumer.poll(Duration.ofSeconds(10), 1);
      assertNotEquals(result.size(), 0);
      result.forEach(x -> assertEquals(x.key().get(), ROW));

      // pause connector
      workerClient.pause(connectorName);
      CommonUtil.await(
          () ->
              result(workerClient.status(connectorName)).connector().state()
                  == ConnectorState.PAUSED,
          Duration.ofSeconds(50));

      // try to receive all data from topic...10 seconds should be enough in this case;
      result = consumer.poll(Duration.ofSeconds(10), Integer.MAX_VALUE);
      result.forEach(x -> assertEquals(x.key().get(), ROW));

      // connector is paused so there is no data
      result = consumer.poll(Duration.ofSeconds(20), 1);
      assertEquals(result.size(), 0);

      // resume connector
      workerClient.resume(connectorName);
      CommonUtil.await(
          () ->
              result(workerClient.status(connectorName)).connector().state()
                  == ConnectorState.RUNNING,
          Duration.ofSeconds(50),
          Duration.ofSeconds(2),
          true);

      // since connector is resumed so some data are generated
      result = consumer.poll(Duration.ofSeconds(20), 1);
      assertNotEquals(result.size(), 0);
    } finally {
      workerClient.delete(connectorName);
    }
  }

  private static final Row ROW = Row.of(Cell.of("f0", 13), Cell.of("f1", false));
}
