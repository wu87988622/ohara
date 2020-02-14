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

package com.island.ohara.common.setting;

import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.common.util.VersionUtils;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * this interface offers the setting definitions to Configurator and Manager. The former uses it to
 * check and serialize the request. The later use the definitions to generate the form to UI users
 * to complete the requests.
 *
 * <p>Noted: there are three definitions are required for all components. Author, Version and
 * Revision. In order to simplify your code, all of them have default value.
 */
public interface WithDefinitions {

  String AUTHOR_KEY = "author";
  int AUTHOR_ORDER = 0;

  static SettingDef authorDefinition(String author) {
    return SettingDef.builder()
        .displayName(AUTHOR_KEY)
        .key(AUTHOR_KEY)
        .documentation("author")
        .group("common")
        .optional(CommonUtils.requireNonEmpty(author))
        .orderInGroup(AUTHOR_ORDER)
        .permission(SettingDef.Permission.READ_ONLY)
        .build();
  }

  SettingDef AUTHOR_DEFINITION = authorDefinition(VersionUtils.USER);

  String VERSION_KEY = "version";
  int VERSION_ORDER = AUTHOR_ORDER + 1;

  static SettingDef versionDefinition(String version) {
    return SettingDef.builder()
        .displayName(VERSION_KEY)
        .key(VERSION_KEY)
        .documentation("version")
        .group("common")
        .optional(CommonUtils.requireNonEmpty(version))
        .orderInGroup(VERSION_ORDER)
        .permission(SettingDef.Permission.READ_ONLY)
        .build();
  }

  SettingDef VERSION_DEFINITION = versionDefinition(VersionUtils.VERSION);

  String REVISION_KEY = "revision";
  int REVISION_ORDER = VERSION_ORDER + 1;

  static SettingDef revisionDefinition(String revision) {
    return SettingDef.builder()
        .displayName(REVISION_KEY)
        .key(REVISION_KEY)
        .documentation("revision")
        .group("common")
        .optional(CommonUtils.requireNonEmpty(revision))
        .orderInGroup(REVISION_ORDER)
        .permission(SettingDef.Permission.READ_ONLY)
        .build();
  }

  SettingDef REVISION_DEFINITION = revisionDefinition(VersionUtils.REVISION);

  /**
   * merge two collections of definitions. the priority of system's definitions is highest so it is
   * able to override the duplicate key in user's definitions. This method also adds version, author
   * and revision to the final definitions if they are absent.
   *
   * @param systemDefinedDefinitions system level definitions
   * @param userDefinedDefinitions user level definitions
   * @return a collections of definitions consisting of both input definitions.
   */
  static Map<String, SettingDef> merge(
      Map<String, SettingDef> systemDefinedDefinitions,
      Map<String, SettingDef> userDefinedDefinitions) {
    Map<String, SettingDef> finalDefinitions = new TreeMap<>(userDefinedDefinitions);
    finalDefinitions.putAll(systemDefinedDefinitions);
    // add system-defined definitions if developers does NOT define them
    finalDefinitions.putIfAbsent(WithDefinitions.AUTHOR_KEY, WithDefinitions.AUTHOR_DEFINITION);
    finalDefinitions.putIfAbsent(WithDefinitions.VERSION_KEY, WithDefinitions.VERSION_DEFINITION);
    finalDefinitions.putIfAbsent(WithDefinitions.REVISION_KEY, WithDefinitions.REVISION_DEFINITION);
    return Collections.unmodifiableMap(finalDefinitions);
  }

  /** @return a unmodifiable collection of definitions */
  Map<String, SettingDef> settingDefinitions();
}
