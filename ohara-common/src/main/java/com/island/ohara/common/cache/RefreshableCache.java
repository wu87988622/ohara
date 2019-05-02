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

import com.island.ohara.common.annotations.Optional;
import com.island.ohara.common.util.Releasable;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A cache with auto-refresh function. It invokes a inner thread to loop the refresh function, which
 * offers the latest key-values to the cache. The use case is that you really really really hate the
 * wait of reloading the new value for timeout key. The inner thread keeps feeding the thread on the
 * key-values supplied by you. Noted that it doesn't guarantee that all your get call won't be
 * blocked anymore since the first call submitted by you is still blocked.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface RefreshableCache<K, V> extends Cache<K, V>, Releasable {
  Logger LOG = LoggerFactory.getLogger(RefreshableCache.class);

  static <K, V> Builder<K, V> builder() {
    return new Builder<>();
  }

  class Builder<K, V> {
    private Cache<K, V> cache = null;
    private Supplier<Map<K, V>> supplier = null;
    private Duration frequency = Duration.ofSeconds(5);

    private Builder() {}

    public Builder<K, V> cache(Cache<K, V> cache) {
      this.cache = Objects.requireNonNull(cache);
      return this;
    }

    public Builder<K, V> supplier(Supplier<Map<K, V>> supplier) {
      this.supplier = Objects.requireNonNull(supplier);
      return this;
    }

    /**
     * @param frequency the time to update cache
     * @return this builder
     */
    @Optional("default value is 5 seconds")
    public Builder<K, V> frequency(Duration frequency) {
      this.frequency = Objects.requireNonNull(frequency);
      return this;
    }

    public RefreshableCache<K, V> build() {
      ExecutorService service = Executors.newSingleThreadExecutor();
      AtomicBoolean closed = new AtomicBoolean(false);
      service.execute(
          () -> {
            while (!closed.get()) {
              try {
                cache.put(supplier.get());
              } catch (Throwable e) {
                LOG.error("failed to update cache", e);
              }
              try {
                TimeUnit.MILLISECONDS.sleep(frequency.toMillis());
              } catch (InterruptedException e) {
                closed.set(true);
              }
            }
            LOG.info("refreshable cache is gone");
          });
      return new RefreshableCache<K, V>() {

        @Override
        public void close() {
          if (closed.compareAndSet(false, true)) {
            service.shutdownNow();
            try {
              if (!service.awaitTermination(30, TimeUnit.SECONDS))
                throw new IllegalStateException("failed to release cache");
            } catch (InterruptedException e) {
              throw new IllegalStateException("failed to release cache", e);
            }
          }
        }

        @Override
        public V get(K key) {
          if (closed.get()) throw new IllegalStateException("cache is closed!!!");
          return cache.get(key);
        }

        @Override
        public void put(Map<? extends K, ? extends V> map) {
          if (closed.get()) throw new IllegalStateException("cache is closed!!!");
          cache.put(map);
        }
      };
    }
  }
}
