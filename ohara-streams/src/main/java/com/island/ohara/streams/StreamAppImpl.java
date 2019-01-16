package com.island.ohara.streams;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class StreamAppImpl {

  private static AtomicBoolean appCalled = new AtomicBoolean(false);
  private static volatile boolean error = false;
  private static volatile RuntimeException exception = null;

  static void launchApplication(final Class<? extends StreamApp> clz, final Object... args) {

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
                  if (args != null) {
                    Constructor<? extends StreamApp> cons =
                        clz.getConstructor(
                            Arrays.asList(args)
                                .stream()
                                .map(c -> c.getClass())
                                .toArray(Class[]::new));
                    app.set(cons.newInstance(args));
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
