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

import com.island.ohara.common.annotations.VisibleForTesting;
import com.island.ohara.common.data.*;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.connector.RowSourceRecord;
import com.island.ohara.kafka.connector.text.TextSourceConverter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A converter to be used to read data from a csv file, and convert to records of Kafka Connect
 * format
 */
public class CsvSourceConverter implements TextSourceConverter {
  @VisibleForTesting static final String CSV_REGEX = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)";
  public static final String CSV_PARTITION_KEY = "csv.file.path";
  public static final String CSV_OFFSET_KEY = "csv.file.line";

  private final String path;
  private final List<String> topics;
  private final List<Column> schema;
  private final Map<String, String> partition;
  private final OffsetCache cache;

  @Override
  public List<RowSourceRecord> convert(Supplier<InputStreamReader> supplier) {
    try (InputStreamReader reader = supplier.get()) {
      Map<Integer, List<Cell<String>>> cellsAndIndex = toCells(reader);
      Map<Integer, Row> rowsAndIndex = transform(cellsAndIndex);
      List<RowSourceRecord> records = toRecords(rowsAndIndex);
      // ok. all data are prepared. let's update the cache
      rowsAndIndex.keySet().forEach(index -> cache.update(path, index));
      return records;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** read all lines from a reader, and then convert them to cells. */
  @VisibleForTesting
  Map<Integer, List<Cell<String>>> toCells(InputStreamReader input) {
    Stream<String> lines = new BufferedReader(input).lines();
    Map<Integer, String> lineAndIndex =
        StreamUtils.zipWithIndex(lines)
            .filter(
                pair -> {
                  int index = pair.left();
                  if (index == 0) return true;
                  return cache.predicate(path, index);
                })
            .collect(Collectors.toMap(Pair::left, Pair::right));

    if (lineAndIndex.size() > 1) {
      String[] header =
          Arrays.stream(lineAndIndex.get(0).split(CSV_REGEX))
              .map(String::trim)
              .toArray(String[]::new);

      return lineAndIndex.entrySet().stream()
          .filter(e -> e.getKey() > 0)
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey,
                  e -> {
                    String line = e.getValue();
                    String[] items = line.split(CSV_REGEX);
                    return IntStream.range(0, items.length)
                        .mapToObj(i -> Cell.of(header[i], items[i].trim()))
                        .collect(Collectors.toList());
                  }));
    }

    return Collections.emptyMap();
  }

  /**
   * transform the input cells to rows as stated by the columns. This method does the following
   * works. 1) filter out the unused cell 2) replace the name by new one 3) convert the string to
   * specified type
   */
  @VisibleForTesting
  Map<Integer, Row> transform(Map<Integer, List<Cell<String>>> indexAndCells) {
    return indexAndCells.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> transform(e.getValue())));
  }

  private Row transform(List<Cell<String>> cells) {
    if (schema.isEmpty()) {
      return Row.of(cells.stream().toArray(Cell[]::new));
    }

    return Row.of(
        schema.stream()
            .sorted(Comparator.comparing(Column::order))
            .map(
                column -> {
                  String value = findCellByName(cells, column.name()).value();
                  Object newValue = convertByType(value, column.dataType());
                  return Cell.of(column.newName(), newValue);
                })
            .toArray(Cell[]::new));
  }

  @VisibleForTesting
  Cell<String> findCellByName(List<Cell<String>> cells, String name) {
    return cells.stream().filter(cell -> cell.name().equals(name)).findFirst().get();
  }

  @VisibleForTesting
  Object convertByType(String value, DataType type) {
    switch (type) {
      case BOOLEAN:
        return Boolean.valueOf(value);
      case BYTE:
        return Byte.valueOf(value);
      case SHORT:
        return Short.valueOf(value);
      case INT:
        return Integer.valueOf(value);
      case LONG:
        return Long.valueOf(value);
      case FLOAT:
        return Float.valueOf(value);
      case DOUBLE:
        return Double.valueOf(value);
      case STRING:
        return value;
      case OBJECT:
        return value;
      default:
        throw new IllegalArgumentException("Unsupported type " + type);
    }
  }

  @VisibleForTesting
  List<RowSourceRecord> toRecords(Map<Integer, Row> rows) {
    return rows.entrySet().stream()
        .map(
            e -> {
              int index = e.getKey();
              Row row = e.getValue();
              return toRecords(row, index);
            })
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  @VisibleForTesting
  List<RowSourceRecord> toRecords(Row row, int index) {
    return this.topics.stream()
        .map(
            t ->
                RowSourceRecord.builder()
                    .sourcePartition(partition)
                    .sourceOffset(Collections.singletonMap(CSV_OFFSET_KEY, index))
                    .row(row)
                    .topicName(t)
                    .build())
        .collect(Collectors.toList());
  }

  public static class Builder {
    // Required parameters
    private String path;
    private List<String> topics;
    private OffsetCache offsetCache;

    // Optional parameters - initialized to default values
    private List<Column> schema = Collections.emptyList();

    public Builder path(String val) {
      path = val;
      return this;
    }

    public Builder topics(List<String> val) {
      topics = val;
      return this;
    }

    public Builder offsetCache(OffsetCache val) {
      offsetCache = val;
      return this;
    }

    @com.island.ohara.common.annotations.Optional("default is empty")
    public Builder schema(List<Column> val) {
      schema = Objects.requireNonNull(val);
      return this;
    }

    public CsvSourceConverter build() {
      Objects.requireNonNull(path);
      CommonUtils.requireNonEmpty(topics);
      Objects.requireNonNull(offsetCache);
      return new CsvSourceConverter(this);
    }
  }

  private CsvSourceConverter(Builder builder) {
    path = builder.path;
    topics = builder.topics;
    schema = builder.schema;
    cache = builder.offsetCache;
    partition = Collections.singletonMap(CSV_PARTITION_KEY, builder.path);
  }
}
