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

package com.island.ohara.common.cache;

import com.google.common.collect.ImmutableMap;
import com.island.ohara.common.rule.OharaTest;
import com.island.ohara.common.util.CommonUtils;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.junit.Test;

public class TestRefreshableCache extends OharaTest {

  @Test(expected = NullPointerException.class)
  public void nullFrequency() {
    RefreshableCache.<String, String>builder().frequency(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullTimeout() {
    RefreshableCache.<String, String>builder().timeout(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullRemoveListener() {
    RefreshableCache.<String, String>builder().preRemoveObserver(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullSupplier() {
    RefreshableCache.<String, String>builder().supplier(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullSupplier2() {
    RefreshableCache.<String, String>builder().build();
  }

  @Test
  public void testDefaultBuilder() {
    RefreshableCache.<String, String>builder()
        .supplier(
            () -> Collections.singletonMap(CommonUtils.randomString(), CommonUtils.randomString()))
        .build()
        .close();
  }

  @Test
  public void testAutoRefresh() throws InterruptedException {
    AtomicInteger supplierCount = new AtomicInteger(0);
    String newKey = CommonUtils.randomString();
    String newValue = CommonUtils.randomString();
    try (RefreshableCache<String, String> cache =
        RefreshableCache.<String, String>builder()
            .supplier(
                () -> {
                  supplierCount.incrementAndGet();
                  return Collections.singletonMap(newKey, newValue);
                })
            .frequency(Duration.ofSeconds(2))
            .build()) {
      Assert.assertEquals(0, supplierCount.get());
      TimeUnit.SECONDS.sleep(3);
      // ok, the cache is auto-refreshed
      Assert.assertEquals(1, supplierCount.get());
      Assert.assertEquals(newValue, cache.get(newKey).get());
      Assert.assertEquals(1, cache.size());
      // insert a key-value
      cache.put(CommonUtils.randomString(), CommonUtils.randomString());
      Assert.assertEquals(2, cache.size());
      // ok, the cache is auto-refreshed again
      TimeUnit.SECONDS.sleep(2);
      Assert.assertTrue(supplierCount.get() >= 2);
      Assert.assertEquals(newValue, cache.get(newKey).get());
      // the older keys are removed
      Assert.assertEquals(1, cache.size());
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testPutsAfterClose() {
    RefreshableCache<String, String> cache = cache();
    cache.close();
    cache.put(Collections.singletonMap(CommonUtils.randomString(), CommonUtils.randomString()));
  }

  @Test
  public void testRequestUpdate() throws InterruptedException {
    AtomicInteger count = new AtomicInteger(0);
    RefreshableCache<String, String> cache =
        RefreshableCache.<String, String>builder()
            .supplier(
                () -> {
                  count.incrementAndGet();
                  return Collections.singletonMap(
                      CommonUtils.randomString(), CommonUtils.randomString());
                })
            // no update before we all die
            .frequency(Duration.ofDays(10000000))
            .build();

    cache.get(CommonUtils.randomString());
    Assert.assertEquals(0, count.get());
    TimeUnit.SECONDS.sleep(2);
    Assert.assertEquals(0, count.get());
    cache.requestUpdate();
    TimeUnit.SECONDS.sleep(2);
    Assert.assertEquals(1, count.get());
  }

  @Test
  public void testClear() throws InterruptedException {
    RefreshableCache<String, String> cache =
        RefreshableCache.<String, String>builder()
            .supplier(
                () ->
                    Collections.singletonMap(
                        CommonUtils.randomString(), CommonUtils.randomString()))
            // no update before we all die
            .frequency(Duration.ofDays(10000000))
            .build();
    cache.requestUpdate();
    TimeUnit.SECONDS.sleep(2);
    Assert.assertEquals(1, cache.size());
    cache.clear();
    Assert.assertEquals(0, cache.size());
  }

  @Test(expected = IllegalStateException.class)
  public void testSnapshotAfterClose() {
    RefreshableCache<String, String> cache = cache();
    cache.close();
    cache.snapshot();
  }

  @Test(expected = IllegalStateException.class)
  public void testClearAfterClose() {
    RefreshableCache<String, String> cache = cache();
    cache.close();
    cache.clear();
  }

  @Test(expected = IllegalStateException.class)
  public void testRequestUpdateAfterClose() {
    RefreshableCache<String, String> cache = cache();
    cache.close();
    cache.requestUpdate();
  }

  @Test(expected = IllegalStateException.class)
  public void testSizeAfterClose() {
    RefreshableCache<String, String> cache = cache();
    cache.close();
    cache.size();
  }

  @Test(expected = IllegalStateException.class)
  public void testGetAfterClose() {
    RefreshableCache<String, String> cache = cache();
    cache.close();
    cache.get(CommonUtils.randomString());
  }

  @Test(expected = IllegalStateException.class)
  public void testPutAfterClose() {
    RefreshableCache<String, String> cache = cache();
    cache.close();
    cache.put(CommonUtils.randomString(), CommonUtils.randomString());
  }

  @Test
  public void testRemove() {
    RefreshableCache<String, String> cache = cache();
    String key = CommonUtils.randomString();
    String value = CommonUtils.randomString();
    cache.put(key, value);
    Assert.assertEquals(value, cache.get(key).get());
    cache.remove(key);
    Assert.assertFalse(cache.get(key).isPresent());
  }

  @Test
  public void testTimeout() throws InterruptedException {
    RefreshableCache<String, String> cache =
        RefreshableCache.<String, String>builder()
            .supplier(
                () ->
                    Collections.singletonMap(
                        CommonUtils.randomString(), CommonUtils.randomString()))
            .frequency(Duration.ofSeconds(1000))
            .frequency(Duration.ofSeconds(2))
            .build();
    String key = CommonUtils.randomString();
    String value = CommonUtils.randomString();
    cache.put(key, value);
    Assert.assertEquals(value, cache.get(key).get());
    TimeUnit.SECONDS.sleep(3);
    Assert.assertFalse(cache.get(key).isPresent());
  }

  @Test
  public void testMissData() throws InterruptedException {
    String key = CommonUtils.randomString();
    String value = CommonUtils.randomString();
    Map<String, String> data =
        ImmutableMap.of(
            key,
            value,
            CommonUtils.randomString(),
            CommonUtils.randomString(),
            CommonUtils.randomString(),
            CommonUtils.randomString(),
            CommonUtils.randomString(),
            CommonUtils.randomString());
    AtomicInteger updateCount = new AtomicInteger(0);
    try (RefreshableCache<String, String> cache =
        RefreshableCache.<String, String>builder()
            .supplier(
                () -> {
                  updateCount.incrementAndGet();
                  return data;
                })
            .frequency(Duration.ofMillis(500))
            .build()) {
      cache.put(data);
      int testTime = 10; // seconds
      int numberOfThreads = 6;
      AtomicBoolean closed = new AtomicBoolean(false);
      AtomicInteger missCount = new AtomicInteger(0);
      ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
      IntStream.range(0, numberOfThreads)
          .forEach(
              index ->
                  service.execute(
                      () -> {
                        try {
                          while (!closed.get()) {
                            if (!cache.get(key).isPresent()) missCount.incrementAndGet();
                            TimeUnit.MILLISECONDS.sleep(100);
                          }
                        } catch (InterruptedException e) {
                          // nothing
                        }
                      }));
      try {
        TimeUnit.SECONDS.sleep(testTime);
      } finally {
        closed.set(true);
        service.shutdownNow();
        Assert.assertTrue(service.awaitTermination(testTime, TimeUnit.SECONDS));
        Assert.assertTrue(updateCount.get() > 0);
        Assert.assertEquals(0, missCount.get());
      }
    }
  }

  @Test
  public void testRemoveListener() throws InterruptedException {
    AtomicInteger updateCount = new AtomicInteger(0);
    String key = CommonUtils.randomString();
    String value = CommonUtils.randomString();
    try (RefreshableCache<String, String> cache =
        RefreshableCache.<String, String>builder()
            .supplier(
                () -> {
                  updateCount.incrementAndGet();
                  return Collections.emptyMap();
                })
            .frequency(Duration.ofSeconds(1))
            .preRemoveObserver((k, v) -> !key.equals(k))
            .build()) {
      cache.put(key, value);
      TimeUnit.SECONDS.sleep(2);
      Assert.assertTrue(updateCount.get() > 0);
      Assert.assertEquals(cache.get(key).get(), value);
    }
  }

  private static RefreshableCache<String, String> cache() {
    return RefreshableCache.<String, String>builder()
        .supplier(
            () -> Collections.singletonMap(CommonUtils.randomString(), CommonUtils.randomString()))
        .frequency(Duration.ofSeconds(2))
        .build();
  }
}
