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

package com.island.ohara.it.agent

import org.junit.Test

/**
  * this test invokes 4 test cases in BasicTestsOfCollie.
  * 1) start a single-node zk cluster
  * 2) start a single-node broker cluster
  * 3) start a single-node worker cluster
  * 4) add an new node to the running worker cluster
  * 5) remove a node from the running worker cluster
  */
class TestSshWorkerCollie extends BasicTestsOfSshCollie {

  @Test
  def test(): Unit = testRemoveNodeToRunningWorkerCluster()
}
