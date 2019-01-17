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

package com.island.ohara.kafka.connector;

import static com.island.ohara.kafka.connector.Constants.BROKER;
import static com.island.ohara.kafka.connector.Constants.INPUT;

import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Serializer;
import com.island.ohara.kafka.Consumer;
import com.island.ohara.kafka.Consumer.Record;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/** Used for testing. */
public class SimpleRowSourceTask extends RowSourceTask {

  private final LinkedBlockingQueue<RowSourceRecord> queue = new LinkedBlockingQueue<>();
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private Consumer<byte[], Row> consumer = null;
  private final ExecutorService executor = Executors.newFixedThreadPool(1);

  @Override
  protected void _start(TaskConfig config) {
    CompletableFuture.runAsync(
        () -> {
          try (Consumer<byte[], Row> consumer =
              Consumer.builder()
                  .connectionProps(config.options().get(BROKER))
                  .groupId(config.name())
                  .topicName(config.options().get(INPUT))
                  .offsetFromBegin()
                  .build(Serializer.BYTES, Serializer.ROW)) {
            this.consumer = consumer;
            while (!closed.get()) {
              List<Record<byte[], Row>> recordList = consumer.poll(Duration.ofSeconds(2));
              recordList
                  .stream()
                  .filter(r -> r.value().isPresent())
                  .map(r -> r.value().get())
                  .flatMap(
                      row ->
                          config
                              .topics()
                              .stream()
                              .map(topic -> RowSourceRecord.builder().row(row).build(topic)))
                  .forEach(
                      r -> {
                        try {
                          queue.put(r);
                        } catch (InterruptedException e) {
                          e.printStackTrace();
                        }
                      });
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        },
        executor);
  }

  @Override
  protected void _stop() {
    if (consumer != null) consumer.wakeup();
    closed.set(true);
  }

  @Override
  protected List<RowSourceRecord> _poll() {

    // Stream Api Support Condition break in JDK9
    List<RowSourceRecord> list = new ArrayList<>();
    RowSourceRecord record;
    while ((record = queue.poll()) != null) list.add(record);

    return list;
  }
}
