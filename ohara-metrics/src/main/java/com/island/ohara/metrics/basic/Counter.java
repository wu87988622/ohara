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
import com.island.ohara.common.annotations.VisibleForTesting;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.common.util.ReleaseOnce;
import com.island.ohara.metrics.BeanChannel;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is an implementation of JMX Bean. We need to implement serializable since we write
 * metrics data to rocksDB.
 */
public final class Counter extends ReleaseOnce implements CounterMBean, Serializable {

  private static final long serialVersionUID = 1L;

  public static Builder builder() {
    return new Builder();
  }

  @VisibleForTesting final boolean needClose;
  @VisibleForTesting final Map<String, String> properties;
  private final String group;
  private final String name;
  private final String document;
  private final String unit;
  private final AtomicLong value = new AtomicLong(0);
  private final long startTime;

  private Counter(
      boolean needClose,
      Map<String, String> properties,
      String group,
      String name,
      String document,
      String unit,
      long startTime,
      long value) {
    this.needClose = needClose;
    this.properties =
        Collections.unmodifiableMap(new HashMap<>(CommonUtils.requireNonEmpty(properties)));
    this.group = CommonUtils.requireNonEmpty(group);
    this.name = CommonUtils.requireNonEmpty(name);
    this.document = CommonUtils.requireNonEmpty(document);
    this.unit = CommonUtils.requireNonEmpty(unit);
    this.startTime = startTime;
    this.value.set(value);
  }

  @Override
  public String group() {
    return group;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String getDocument() {
    return document;
  }

  @Override
  public String getUnit() {
    return unit;
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
    if (obj instanceof Counter) {
      Counter another = (Counter) obj;
      return another.group().equals(group())
          && another.name().equals(name())
          && another.getStartTime() == getStartTime()
          && another.getValue() == getValue()
          && another.getUnit().equals(getUnit());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(group(), name(), getValue(), getStartTime(), getUnit());
  }

  @Override
  public String toString() {
    return "group:"
        + group()
        + " name:"
        + name()
        + " start:"
        + getStartTime()
        + " value:"
        + getValue()
        + " unit:"
        + getUnit();
  }

  @Override
  protected void doClose() {
    if (needClose) BeanChannel.unregister(CounterMBean.DOMAIN, properties);
  }

  public static class Builder {
    private String id;
    private String group;
    private String name;
    private String unit = "N/A";
    private String document = "there is no document for this counter...";
    private long value = 0;
    private long startTime = CommonUtils.current();

    private Builder() {}

    @Optional("default is random string")
    public Builder id(String id) {
      this.id = CommonUtils.requireNonEmpty(id);
      return this;
    }

    @Optional("default is equal to name")
    public Builder group(String group) {
      this.group = CommonUtils.requireNonEmpty(group);
      return this;
    }

    public Builder name(String name) {
      this.name = CommonUtils.requireNonEmpty(name);
      if (CommonUtils.isEmpty(group)) group = this.name;
      return this;
    }

    @Optional("default is zero")
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

    @Optional("default is N/A")
    public Builder unit(String unit) {
      this.unit = CommonUtils.requireNonEmpty(unit);
      return this;
    }

    private void checkArgument() {
      CommonUtils.requireNonEmpty(group);
      CommonUtils.requireNonEmpty(name);
      CommonUtils.requireNonEmpty(unit);
      CommonUtils.requireNonEmpty(document);
    }

    /**
     * create a mutable counter. NOTED: this method is NOT public since we disallow user to create a
     * counter without registry.
     *
     * @return Counter
     */
    @VisibleForTesting
    Counter build() {
      return build(false);
    }

    /**
     * create and register a mutable counter.
     *
     * @return Counter
     */
    public Counter register() {
      Counter counter = build(true);
      return BeanChannel.<Counter>register()
          .domain(DOMAIN)
          .properties(counter.properties)
          .beanObject(counter)
          .run();
    }

    /**
     * Create a counter with a flag indicating the action of unregistering beans from local jvm.
     *
     * @param needClose if true, the close() method will invoke unregister also.
     * @return counter
     */
    private Counter build(boolean needClose) {
      checkArgument();
      Map<String, String> properties = new HashMap<>();
      properties.put(TYPE_KEY, TYPE_VALUE);
      // the metrics tools (for example, jmc) can distinguish the counter via the name.
      properties.put(GROUP_KEY, group);
      // the metrics tools (for example, jmc) can distinguish the counter via the name.
      properties.put(NAME_KEY, name);
      // we use a random string to avoid duplicate jmx
      // This property is required since kafka worker may create multiple tasks on same worker node.
      // If we don't have this id, the multiple tasks will fail since the duplicate counters.
      properties.put(ID_KEY, CommonUtils.isEmpty(id) ? CommonUtils.randomString() : id);
      return new Counter(needClose, properties, group, name, document, unit, startTime, value);
    }
  }
}
