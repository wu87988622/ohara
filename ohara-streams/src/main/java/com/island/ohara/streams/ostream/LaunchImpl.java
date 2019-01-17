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

import com.island.ohara.streams.StreamApp;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class LaunchImpl {

  private static AtomicBoolean appCalled = new AtomicBoolean(false);
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
                  if (params != null) {
                    Constructor<? extends StreamApp> cons =
                        clz.getConstructor(
                            Arrays.stream(params).map(Object::getClass).toArray(Class[]::new));
                    app.set(cons.newInstance(params));
                  } else {
                    Constructor<? extends StreamApp> cons = clz.getConstructor();
                    app.set(cons.newInstance());
                  }
                  final StreamApp theApp = app.get();

                  theApp.init();
                  theApp.start();
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
    thread.setName("Island-StreamApp");
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
