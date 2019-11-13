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

import com.island.ohara.common.data.Pair;
import com.island.ohara.common.json.JsonUtils;
import com.island.ohara.common.setting.SettingDef;
import com.island.ohara.common.util.CommonUtils;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.reflections.Reflections;

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

  String OUTPUT_FOLDER_KEY = "output";
  String POSTFIX = "definitions";

  /**
   * this main function is used to expose the definitions from input files.
   *
   * @param lines args
   */
  static void main(String[] lines) {
    Map<String, String> args = CommonUtils.parse(Arrays.asList(lines));
    if (!args.containsKey(OUTPUT_FOLDER_KEY))
      throw new RuntimeException("the config of " + OUTPUT_FOLDER_KEY + " is required");
    File folder = new File(args.get(OUTPUT_FOLDER_KEY));
    if (folder.exists() && !folder.isDirectory())
      throw new RuntimeException(
          "the file:" + args.get(OUTPUT_FOLDER_KEY) + " is already existed!!!");
    if (!folder.exists() && !folder.mkdirs())
      throw new RuntimeException("fail to create folder on " + args.get(OUTPUT_FOLDER_KEY));
    Reflections reflections = new Reflections();
    List<Pair<String, List<SettingDef>>> classes =
        reflections.getSubTypesOf(WithDefinitions.class).stream()
            .filter(c -> !Modifier.isAbstract(c.getModifiers()))
            .map(
                c -> {
                  try {
                    return Optional.of(Pair.of(c.getName(), c.newInstance().definitions()));
                  } catch (Throwable t) {
                    return Optional.<Pair<String, List<SettingDef>>>empty();
                  }
                })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

    if (!classes.isEmpty()) {
      classes.forEach(
          pair -> {
            File file = new File(folder, pair.left() + "." + POSTFIX);
            try (FileWriter writer = new FileWriter(file)) {
              writer.write(JsonUtils.toString(pair.right()));
            } catch (Throwable e) {
              // just swallow the exception
            }
          });
    }
  }
}
