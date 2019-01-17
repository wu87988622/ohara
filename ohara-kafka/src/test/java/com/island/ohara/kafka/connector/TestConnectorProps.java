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

package com.island.ohara.kafka.connector;

import com.island.ohara.common.rule.SmallTest;
import java.util.Collections;
import org.junit.Test;

public class TestConnectorProps extends SmallTest {

  @Test
  public void testNoTopicsInSourceConnector() {
    DumbSource connector = new DumbSource();
    // lack topics string
    assertException(IllegalArgumentException.class, () -> connector.start(Collections.emptyMap()));
  }

  @Test
  public void testNoTopicsInSinkConnector() {
    DumbSink connector = new DumbSink();
    // lack topics string
    assertException(IllegalArgumentException.class, () -> connector.start(Collections.emptyMap()));
  }

  @Test
  public void testNoTopicsInSourceTask() {
    DumbSourceTask task = new DumbSourceTask();
    assertException(IllegalArgumentException.class, () -> task.start(Collections.emptyMap()));
  }

  @Test
  public void testNoTopicsInSinkTask() {
    DumbSinkTask task = new DumbSinkTask();
    assertException(IllegalArgumentException.class, () -> task.start(Collections.emptyMap()));
  }

  // TODO: add tests against adding interval key manually...see OHARA-588
}
