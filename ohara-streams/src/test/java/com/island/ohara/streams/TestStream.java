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

package com.island.ohara.streams;

import com.island.ohara.common.data.Pair;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.rule.OharaTest;
import com.island.ohara.common.setting.TopicKey;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.streams.config.StreamDefUtils;
import com.island.ohara.streams.config.StreamSetting;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class TestStream extends OharaTest {

  @Test
  public void testCanFindCustomClassEntryFromInnerClass() {
    CustomStream app = new CustomStream();
    TopicKey fromKey = TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString());
    TopicKey toKey = TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString());

    // initial all required environment
    StreamTestUtils.setOharaEnv(
        java.util.stream.Stream.of(
                Pair.of(StreamDefUtils.NAME_DEFINITION.key(), CommonUtils.randomString(5)),
                Pair.of(StreamDefUtils.BROKER_DEFINITION.key(), CommonUtils.randomString()),
                Pair.of(
                    StreamDefUtils.FROM_TOPIC_KEYS_DEFINITION.key(),
                    TopicKey.toJsonString(Collections.singletonList(fromKey))),
                Pair.of(
                    StreamDefUtils.TO_TOPIC_KEYS_DEFINITION.key(),
                    TopicKey.toJsonString(Collections.singletonList(toKey))))
            .collect(Collectors.toMap(Pair::left, Pair::right)));
    Stream.execute(app.getClass());
  }

  public static class CustomStream extends Stream {
    final AtomicInteger counter = new AtomicInteger();

    @Override
    public void init() {
      int res = counter.incrementAndGet();
      // Stream should call init() first
      Assert.assertEquals(1, res);
    }

    @Override
    public void start(OStream<Row> ostream, StreamSetting streamSetting) {
      int res = counter.incrementAndGet();
      // Stream should call start() after init()
      Assert.assertEquals(2, res);
    }
  }
}
