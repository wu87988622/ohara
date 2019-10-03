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

import com.island.ohara.common.setting.SettingDef;
import java.util.List;

public interface WithDefinitions {

  /** @return a unmodifiable collection of definitions */
  List<SettingDef> definitions();

  /**
   * the "column" describes the serialization ruleS of input/output data. However, not all
   * connectors count on it. Keeping a complicated but useless definition is weird to users. Hence,
   * we offer a way to "remove" it from connectors.
   *
   * @return true if your connector counts on column. Otherwise, false.
   */
  default boolean needColumnDefinition() {
    return true;
  }
}
