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

import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.common.util.Releasable;
import com.island.ohara.testing.With3Brokers;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.After;
import org.junit.Test;

public class TestTopicAdmin extends With3Brokers {
  private final TopicAdmin client = TopicAdmin.of(testUtil().brokersConnProps());

  private void waitPartitions(String topicName, int numberOfPartitions) {
    CommonUtils.await(
        () -> {
          try {
            return client
                    .topicDescription(topicName)
                    .toCompletableFuture()
                    .get()
                    .numberOfPartitions()
                == numberOfPartitions;
          } catch (Exception e) {
            return false;
          }
        },
        Duration.ofSeconds(30));
  }

  @Test
  public void testAddPartitions() throws ExecutionException, InterruptedException {
    String topicName = CommonUtils.randomString(10);
    int numberOfPartitions = 1;
    short numberOfReplications = 1;
    client
        .topicCreator()
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .topicName(topicName)
        .create()
        .toCompletableFuture()
        .get();
    waitPartitions(topicName, numberOfPartitions);
    assertEquals(
        numberOfPartitions,
        client.topicDescription(topicName).toCompletableFuture().get().numberOfPartitions());

    numberOfPartitions = 2;
    client.createPartitions(topicName, numberOfPartitions).toCompletableFuture().get();
    waitPartitions(topicName, numberOfPartitions);
    assertEquals(
        numberOfPartitions,
        client.topicDescription(topicName).toCompletableFuture().get().numberOfPartitions());
    // decrease the number
    assertException(
        Exception.class, () -> client.createPartitions(topicName, 1).toCompletableFuture().get());
    // alter an nonexistent topic
    assertException(
        NoSuchElementException.class,
        () -> client.createPartitions("Xxx", 2).toCompletableFuture().get(),
        true);
  }

  @Test
  public void testCreate() throws ExecutionException, InterruptedException {
    String topicName = CommonUtils.randomString(10);
    int numberOfPartitions = 2;
    short numberOfReplications = (short) 2;
    client
        .topicCreator()
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .topicName(topicName)
        .create()
        .toCompletableFuture()
        .get();
    waitPartitions(topicName, numberOfPartitions);
    TopicDescription topicInfo = client.topicDescription(topicName).toCompletableFuture().get();

    assertEquals(topicName, topicInfo.name());

    assertEquals(numberOfPartitions, topicInfo.numberOfPartitions());
    assertEquals(numberOfReplications, topicInfo.numberOfReplications());

    assertEquals(
        topicInfo,
        client.topicDescriptions().toCompletableFuture().get().stream()
            .filter(t -> t.name().equals(topicName))
            .findFirst()
            .get());

    client.deleteTopic(topicName).toCompletableFuture().get();
    assertFalse(client.exist(topicName).toCompletableFuture().get());
  }

  @Test
  public void testTopicOptions() throws ExecutionException, InterruptedException {
    String topicName = CommonUtils.randomString(10);
    int numberOfPartitions = 2;
    short numberOfReplications = (short) 2;
    Map<String, String> options =
        Collections.singletonMap(
            TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE);
    client
        .topicCreator()
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .options(options)
        .topicName(topicName)
        .create()
        .toCompletableFuture()
        .get();
    waitPartitions(topicName, numberOfPartitions);

    TopicDescription topicInfo = client.topicDescription(topicName).toCompletableFuture().get();

    assertEquals(topicName, topicInfo.name());

    assertEquals(numberOfPartitions, topicInfo.numberOfPartitions());
    assertEquals(numberOfReplications, topicInfo.numberOfReplications());

    assertEquals(
        TopicConfig.CLEANUP_POLICY_DELETE,
        topicInfo.options().stream()
            .filter(x -> Objects.equals(x.key(), TopicConfig.CLEANUP_POLICY_CONFIG))
            .collect(Collectors.toList())
            .get(0)
            .value());
  }

  @After
  public void cleanup() throws ExecutionException, InterruptedException {
    client
        .topicDescriptions()
        .toCompletableFuture()
        .get()
        .forEach(
            t -> {
              try {
                client.deleteTopic(t.name()).toCompletableFuture().get();
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
    Releasable.close(client);
  }
}
