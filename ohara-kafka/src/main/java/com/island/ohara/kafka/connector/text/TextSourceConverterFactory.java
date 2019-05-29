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

package com.island.ohara.kafka.connector.text;

import com.island.ohara.kafka.connector.RowSourceContext;
import com.island.ohara.kafka.connector.TaskSetting;
import com.island.ohara.kafka.connector.text.csv.CsvSourceConverterFactory;

/**
 * Factory for creating {@link TextSourceConverter} instances, which can used in keep something
 * between each converter. e.g. like {@link CsvSourceConverterFactory} keeps a CsvOffsetCache
 * object.
 */
@FunctionalInterface
public interface TextSourceConverterFactory {
  enum TextType {
    CSV,
  }

  static TextSourceConverterFactory of(TaskSetting config, TextType type) {
    switch (type) {
      case CSV:
        return new CsvSourceConverterFactory(config);
      default:
        throw new IllegalArgumentException("Unsupported type " + type);
    }
  }

  /**
   * Create a converter, we expect each converter to process only one file, so before the
   * conversion, we can use path to get the previous offset from the RowSourceContext.
   *
   * @param context a wrap to kafka SourceTaskContext
   * @param path the name of input file
   * @return converter a text source converter
   */
  TextSourceConverter newConverter(RowSourceContext context, String path);
}
