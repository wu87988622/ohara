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

package com.island.ohara.client.configurator

import com.island.ohara.client.configurator.v0.ConnectorApi
import com.island.ohara.client.configurator.v0.ConnectorApi.{ConnectorDescription, ConnectorState}
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorState._
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.SettingDefinition
import org.junit.Test
import org.scalatest.Matchers
import spray.json.{JsObject, JsString}

class TestConnectorApi extends SmallTest with Matchers {

  @Test
  def testState(): Unit = {
    ConnectorState.all shouldBe Seq(
      UNASSIGNED,
      RUNNING,
      PAUSED,
      FAILED,
      DESTROYED
    ).sortBy(_.name)
  }

  @Test
  def testStateJson(): Unit = {
    ConnectorState.all.foreach(
      state =>
        ConnectorApi.CONNECTOR_STATE_JSON_FORMAT
          .read(ConnectorApi.CONNECTOR_STATE_JSON_FORMAT.write(state)) shouldBe state)
  }

  @Test
  def renderJsonWithoutAnyRequiredFields(): Unit = {
    val response = ConnectorDescription(
      id = CommonUtils.randomString(),
      settings = Map(CommonUtils.randomString() -> JsString(CommonUtils.randomString())),
      state = None,
      error = None,
      lastModified = CommonUtils.current()
    )
    // pass
    ConnectorApi.CONNECTOR_DESCRIPTION_JSON_FORMAT.write(response)
  }

  @Test
  def renderJsonWithConnectorClass(): Unit = {
    val className = CommonUtils.randomString()
    val response = ConnectorDescription(
      id = CommonUtils.randomString(),
      settings = Map(SettingDefinition.CONNECTOR_CLASS_DEFINITION.key() -> JsString(className)),
      state = None,
      error = None,
      lastModified = CommonUtils.current()
    )
    ConnectorApi.CONNECTOR_DESCRIPTION_JSON_FORMAT
      .write(response)
      .asInstanceOf[JsObject]
      // previous name
      .fields("className")
      .asInstanceOf[JsString]
      .value shouldBe className
  }
}
