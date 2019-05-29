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

package com.island.ohara.kafka.connector.text.csv;

import com.island.ohara.common.data.Column;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.connector.RowSourceContext;
import com.island.ohara.kafka.connector.TaskSetting;
import com.island.ohara.kafka.connector.text.TextSourceConverter;
import com.island.ohara.kafka.connector.text.TextSourceConverterFactory;
import java.util.List;
import java.util.Objects;

/** This ia a helper to Create the converter. */
public class CsvSourceConverterFactory implements TextSourceConverterFactory {
  private final List<String> topics;
  private final List<Column> schema;
  private final CsvOffsetCache offsetCache;

  public CsvSourceConverterFactory(TaskSetting config) {
    Objects.requireNonNull(config);
    this.topics = CommonUtils.requireNonEmpty(config.topicNames());
    this.schema = config.columns();
    this.offsetCache = new CsvOffsetCache();
  }

  @Override
  public TextSourceConverter newConverter(RowSourceContext context, String path) {
    // update cache
    offsetCache.update(context, path);

    return new CsvSourceConverter.Builder()
        .path(path)
        .topics(topics)
        .offsetCache(offsetCache)
        .schema(schema)
        .build();
  }
}
