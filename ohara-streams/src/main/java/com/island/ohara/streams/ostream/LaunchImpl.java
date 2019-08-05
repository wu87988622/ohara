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

package com.island.ohara.streams.ostream;

import com.island.ohara.common.data.Row;
import com.island.ohara.streams.OStream;
import com.island.ohara.streams.StreamApp;
import com.island.ohara.streams.config.StreamDefinitions;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class LaunchImpl {

  private static final AtomicBoolean appCalled = new AtomicBoolean(false);
  private static volatile boolean error = false;
  private static volatile RuntimeException exception = null;

  public static void launchApplication(
      final Class<? extends StreamApp> clz, final Object... params) {

    if (appCalled.getAndSet(true)) {
      throw new IllegalStateException("StreamApp could only be called once in each thread");
    }

    // Create thread and wait for that thread to finish
    final CountDownLatch latch = new CountDownLatch(1);
    Thread thread =
        new Thread(
            () -> {
              try {
                final AtomicReference<StreamApp> app = new AtomicReference<>();
                if (!error) {
                  if (params != null
                      && params.length > 0
                      && !params[0].equals(StreamsConfig.STREAMAPP_DRY_RUN)) {
                    Constructor<? extends StreamApp> cons =
                        clz.getConstructor(
                            Stream.of(params).map(Object::getClass).toArray(Class[]::new));
                    app.set(cons.newInstance(params));
                  } else {
                    Constructor<? extends StreamApp> cons = clz.getConstructor();
                    app.set(cons.newInstance());
                  }
                  final StreamApp theApp = app.get();

                  Method method =
                      clz.getSuperclass().getDeclaredMethod(StreamsConfig.STREAMAPP_CONFIG_NAME);
                  StreamDefinitions streamDefinitions = (StreamDefinitions) method.invoke(theApp);

                  if (params != null
                      && params.length == 1
                      && params[0].equals(StreamsConfig.STREAMAPP_DRY_RUN)) {
                    System.out.println(streamDefinitions.toString());
                  } else {
                    OStream<Row> ostream =
                        OStream.builder()
                            .appid(streamDefinitions.get(StreamsConfig.STREAMAPP_APPID))
                            .bootstrapServers(
                                streamDefinitions.get(StreamsConfig.STREAMAPP_BOOTSTRAP_SERVERS))
                            .fromTopicWith(
                                streamDefinitions.get(StreamsConfig.STREAMAPP_FROM_TOPICS),
                                Serdes.ROW,
                                Serdes.BYTES)
                            .toTopicWith(
                                streamDefinitions.get(StreamsConfig.STREAMAPP_TO_TOPICS),
                                Serdes.ROW,
                                Serdes.BYTES)
                            .build();
                    theApp.init();
                    theApp.start(ostream, streamDefinitions);
                  }
                }
              } catch (RuntimeException e) {
                error = true;
                exception = e;
              } catch (Exception e) {
                error = true;
                exception = new RuntimeException("StreamApp exception", e);
              } finally {
                latch.countDown();
              }
            });
    thread.setContextClassLoader(clz.getClassLoader());
    thread.setName("Ohara-StreamApp");
    thread.start();

    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    if (exception != null) {
      throw exception;
    }
  }
}
