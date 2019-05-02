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

import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.common.util.CommonUtils;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public class TestRefreshableCache extends SmallTest {

  @Test(expected = NullPointerException.class)
  public void nullFrequency() {
    RefreshableCache.<String, String>builder().frequency(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullSupplier() {
    RefreshableCache.<String, String>builder().supplier(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullCache() {
    RefreshableCache.<String, String>builder().cache(null);
  }

  @Test
  public void testAutoRefresh() throws InterruptedException {
    AtomicInteger count = new AtomicInteger(0);
    String newKey = CommonUtils.randomString();
    String newValue = CommonUtils.randomString();
    try (RefreshableCache<String, String> cache =
        RefreshableCache.<String, String>builder()
            .cache(
                Cache.<String, String>builder()
                    .timeout(Duration.ofSeconds(2))
                    .fetcher(
                        key -> {
                          count.incrementAndGet();
                          return CommonUtils.randomString();
                        })
                    .build())
            .supplier(() -> Collections.singletonMap(newKey, newValue))
            .frequency(Duration.ofSeconds(2))
            .build()) {
      TimeUnit.SECONDS.sleep(3);
      // ok, the cache is auto-refreshed
      cache.get(newKey);
      Assert.assertEquals(0, count.get());
    }
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

  @Test(expected = IllegalStateException.class)
  public void testPutsAfterClose() {
    RefreshableCache<String, String> cache = cache();
    cache.close();
    cache.put(Collections.singletonMap(CommonUtils.randomString(), CommonUtils.randomString()));
  }

  private static RefreshableCache<String, String> cache() {
    return RefreshableCache.<String, String>builder()
        .cache(
            Cache.<String, String>builder()
                .timeout(Duration.ofSeconds(2))
                .fetcher(key -> CommonUtils.randomString())
                .build())
        .supplier(
            () -> Collections.singletonMap(CommonUtils.randomString(), CommonUtils.randomString()))
        .frequency(Duration.ofSeconds(2))
        .build();
  }
}
