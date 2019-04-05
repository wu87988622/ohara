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

package com.island.ohara.metrics.basic;

import com.island.ohara.common.annotations.Optional;
import com.island.ohara.common.util.CommonUtils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class Counter implements CounterMBean {

  public static Builder builder() {
    return new Builder();
  }

  private final String name;
  private final String document;
  private final AtomicLong value = new AtomicLong(0);
  private final long startTime;

  private Counter(String name, String document, long startTime, long value) {
    this.name = CommonUtils.requireNonEmpty(name);
    this.document = CommonUtils.requireNonEmpty(document);
    this.startTime = startTime;
    this.value.set(value);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String getDocument() {
    return document;
  }

  /**
   * Atomically increments by one the current value.
   *
   * @return the updated value
   */
  public long incrementAndGet() {
    return value.incrementAndGet();
  }

  /**
   * Atomically increments by one the current value.
   *
   * @return the previous value
   */
  public long getAndIncrement() {
    return value.getAndIncrement();
  }

  /**
   * Atomically decrements by one the current value.
   *
   * @return the updated value
   */
  public long decrementAndGet() {
    return value.decrementAndGet();
  }

  /**
   * Atomically decrements by one the current value.
   *
   * @return the previous value
   */
  public long getAndDecrement() {
    return value.getAndDecrement();
  }

  /**
   * Atomically adds the given value to the current value.
   *
   * @param delta the value to add
   * @return the updated value
   */
  public long addAndGet(long delta) {
    return value.addAndGet(delta);
  }

  /**
   * Atomically adds the given value to the current value.
   *
   * @param delta the value to add
   * @return the previous value
   */
  public long getAndAdd(long delta) {
    return value.getAndAdd(delta);
  }

  /**
   * Atomically sets to the given value and returns the old value.
   *
   * @param newValue the new value
   * @return the previous value
   */
  public long getAndSet(long newValue) {
    return value.getAndSet(newValue);
  }

  /**
   * Sets to the given value.
   *
   * @param newValue the new value
   * @return the new value
   */
  public long setAndGet(long newValue) {
    value.set(newValue);
    return newValue;
  }

  @Override
  public long getStartTime() {
    return startTime;
  }

  @Override
  public long getValue() {
    return value.get();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof CounterMBean) {
      CounterMBean another = (CounterMBean) obj;
      return another.name().equals(name())
        && another.getStartTime() == getStartTime()
        && another.getValue() == getValue();
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name(), getValue(), getStartTime());
  }

  @Override
  public String toString() {
    return "name:" + name()
      + " start:" + getStartTime()
      + " value:" + getValue();
  }


  static class Builder {
    private String name;
    private String document = "there is no document for this counter...";
    private long value = 0;
    private long startTime = CommonUtils.current();

    private Builder() {
    }

    public Builder name(String name) {
      this.name = CommonUtils.requireNonEmpty(name);
      return this;
    }

    @Optional("default is zeon")
    public Builder value(long value) {
      this.value = value;
      return this;
    }

    @Optional("default is current time")
    public Builder startTime(long startTime) {
      this.startTime = startTime;
      return this;
    }

    @Optional("default is no document")
    public Builder document(String document) {
      this.document = CommonUtils.requireNonEmpty(document);
      return this;
    }

    public Counter build() {
      return new Counter(name, document, startTime, value);
    }
  }
}
