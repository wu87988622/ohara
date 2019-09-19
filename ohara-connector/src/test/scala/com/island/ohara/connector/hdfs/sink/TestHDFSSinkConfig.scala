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

package com.island.ohara.connector.hdfs.sink

import com.island.ohara.common.rule.OharaTest
import com.island.ohara.kafka.connector.TaskSetting
import org.junit.Test
import org.scalatest.Matchers

import scala.collection.JavaConverters._

class TestHDFSSinkConfig extends OharaTest with Matchers {
  private[this] val HDFS_URL_VALUE = "hdfs://test:9000"

  private[this] def hdfsConfig(settings: Map[String, String]): HDFSSinkProps =
    HDFSSinkProps(TaskSetting.of(settings.asJava))

  @Test
  def testGetDataDir(): Unit = {
    val hdfsSinkConfig: HDFSSinkProps = hdfsConfig(Map(HDFS_URL_KEY -> HDFS_URL_VALUE))

    hdfsSinkConfig.hdfsURL shouldBe HDFS_URL_VALUE
  }
}
