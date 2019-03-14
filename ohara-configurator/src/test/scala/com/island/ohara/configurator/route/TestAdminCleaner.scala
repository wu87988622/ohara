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

package com.island.ohara.configurator.route

import java.util.concurrent.atomic.AtomicBoolean

import com.island.ohara.client.kafka.TopicAdmin
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.mockito.Mockito._
import org.scalatest.Matchers

class TestAdminCleaner extends SmallTest with Matchers {

  @Test
  def testTimeout(): Unit = {
    val timeout = 2000
    val cleaner = new CollieUtils.AdminCleaner(timeout)
    val fakeAdmin2 = mock(classOf[TopicAdmin])
    val _closed = new AtomicBoolean(false)
    when(fakeAdmin2.closed()).thenReturn(false)
    when(fakeAdmin2.close()).thenAnswer(_ => _closed.set(true))
    try {
      cleaner.add(fakeAdmin2)
      CommonUtils.await(() => _closed.get(), java.time.Duration.ofMillis(timeout * 5))
    } finally {
      cleaner.close()
      cleaner.executor.isTerminated shouldBe true
    }
  }

  @Test
  def testClose(): Unit = {
    val cleaner = new CollieUtils.AdminCleaner(2000)
    cleaner.close()
    an[IllegalArgumentException] should be thrownBy cleaner.add(mock(classOf[TopicAdmin]))
  }

  @Test
  def testClose2(): Unit = {
    val fakeAdmin2 = mock(classOf[TopicAdmin])
    val _closed = new AtomicBoolean(false)
    when(fakeAdmin2.closed()).thenReturn(false)
    when(fakeAdmin2.close()).thenAnswer(_ => _closed.set(true))
    val cleaner = new CollieUtils.AdminCleaner(2000)
    try cleaner.add(fakeAdmin2)
    finally cleaner.close()
    _closed.get() shouldBe true
  }
}
