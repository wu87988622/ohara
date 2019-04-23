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

package com.island.ohara.metrics.kafka;

import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.metrics.BeanObject;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TopicMeter {
  // -------------------------[property keys]-------------------------//
  private static final String DOMAIN = "kafka.server";
  private static final String TYPE_KEY = "type";
  private static final String TYPE_VALUE = "BrokerTopicMetrics";
  private static final String TOPIC_KEY = "topic";
  private static final String NAME_KEY = "name";
  // -------------------------[attribute keys]-------------------------//
  private static final String COUNT_KEY = "Count";
  private static final String EVENT_TYPE_KEY = "EventType";
  private static final String FIFTEEN_MINUTE_RATE_KEY = "FifteenMinuteRate";
  private static final String FIVE_MINUTE_RATE_KEY = "FiveMinuteRate";
  private static final String MEAN_RATE_KEY = "MeanRate";
  private static final String ONE_MINUTE_RATE_KEY = "OneMinuteRate";
  private static final String RATE_UNIT_KEY = "RateUnit";

  /** reference to kafka.server.BrokerTopicStats */
  private static final List<String> NAME_VALUES =
      Arrays.asList(
          "MessagesInPerSec",
          "BytesInPerSec",
          "BytesOutPerSec",
          "BytesRejectedPerSec",
          "FailedProduceRequestsPerSec",
          "FailedFetchRequestsPerSec",
          "TotalProduceRequestsPerSec",
          "TotalFetchRequestsPerSec",
          "FetchMessageConversionsPerSec",
          "ProduceMessageConversionsPerSec");

  public static boolean is(BeanObject obj) {
    return obj.domainName().equals(DOMAIN)
        && TYPE_VALUE.equals(obj.properties().get(TYPE_KEY))
        && obj.properties().containsKey(TOPIC_KEY)
        && obj.properties().containsKey(NAME_KEY)
        && NAME_VALUES.stream().anyMatch(name -> name.equals(obj.properties().get(NAME_KEY)));
  }

  public static TopicMeter of(BeanObject obj) {
    return new TopicMeter(
        obj.properties().get(TOPIC_KEY),
        obj.properties().get(NAME_KEY),
        (long) obj.attributes().get(COUNT_KEY),
        (String) obj.attributes().get(EVENT_TYPE_KEY),
        (double) obj.attributes().get(FIFTEEN_MINUTE_RATE_KEY),
        (double) obj.attributes().get(FIVE_MINUTE_RATE_KEY),
        (double) obj.attributes().get(MEAN_RATE_KEY),
        (double) obj.attributes().get(ONE_MINUTE_RATE_KEY),
        (TimeUnit) obj.attributes().get(RATE_UNIT_KEY));
  }

  private final String topicName;
  private final String name;
  private final long count;
  private final String eventType;
  private final double fifteenMinuteRate;
  private final double fiveMinuteRate;
  private final double meanRate;
  private final double oneMinuteRate;
  private final TimeUnit rateUnit;

  private TopicMeter(
      String topicName,
      String name,
      long count,
      String eventType,
      double fifteenMinuteRate,
      double fiveMinuteRate,
      double meanRate,
      double oneMinuteRate,
      TimeUnit rateUnit) {
    this.topicName = CommonUtils.requireNonEmpty(topicName);
    this.name = CommonUtils.requireNonEmpty(name);
    this.eventType = CommonUtils.requireNonEmpty(eventType);
    this.count = count;
    this.fifteenMinuteRate = fifteenMinuteRate;
    this.fiveMinuteRate = fiveMinuteRate;
    this.meanRate = meanRate;
    this.oneMinuteRate = oneMinuteRate;
    this.rateUnit = Objects.requireNonNull(rateUnit);
  }

  public String topicName() {
    return topicName;
  }

  public String name() {
    return name;
  }

  public long count() {
    return count;
  }

  public String eventType() {
    return eventType;
  }

  public double fifteenMinuteRate() {
    return fifteenMinuteRate;
  }

  public double fiveMinuteRate() {
    return fiveMinuteRate;
  }

  public double meanRate() {
    return meanRate;
  }

  public double oneMinuteRate() {
    return oneMinuteRate;
  }

  public TimeUnit rateUnit() {
    return rateUnit;
  }
}
