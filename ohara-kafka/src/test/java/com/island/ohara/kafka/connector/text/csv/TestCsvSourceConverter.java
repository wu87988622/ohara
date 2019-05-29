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

import com.island.ohara.common.data.Cell;
import com.island.ohara.common.data.Column;
import com.island.ohara.common.data.DataType;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.rule.SmallTest;
import com.island.ohara.kafka.connector.RowSourceContext;
import com.island.ohara.kafka.connector.RowSourceRecord;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestCsvSourceConverter extends SmallTest {
  private final List<String> topicNames = Arrays.asList("T1", "T2");
  private final List<Column> schema =
      Arrays.asList(
          Column.builder().name("cf1").dataType(DataType.STRING).order(0).build(),
          Column.builder().name("cf2").dataType(DataType.STRING).order(1).build(),
          Column.builder().name("cf3").dataType(DataType.STRING).order(2).build());

  private String path;
  private File tempFile;
  private CsvSourceConverter converter;
  private Map<Integer, List<Cell<String>>> data;

  @Before
  public void setup() throws IOException {
    tempFile = File.createTempFile("test", "csv");
    path = tempFile.getName();
  }

  private CsvSourceConverter createConverter() {
    return new CsvSourceConverter.Builder()
        .path(path)
        .topics(topicNames)
        .offsetCache(new FakeOffsetCache())
        .build();
  }

  private CsvSourceConverter createConverter(List<Column> schema) {
    return new CsvSourceConverter.Builder()
        .path(path)
        .topics(topicNames)
        .offsetCache(new FakeOffsetCache())
        .schema(schema)
        .build();
  }

  private Map<Integer, List<Cell<String>>> setupInputData() {
    String[] header = new String[] {"cf1", "cf2", "cf3"};
    String[] line1 = new String[] {"a", "b", "c"};
    String[] line2 = new String[] {"a", "d", "c"};
    String[] line3 = new String[] {"a", "f", "c"};

    String lineSeparator = System.getProperty("line.separator");

    try (FileWriter writer = new FileWriter(tempFile)) {
      writer.write(StringUtils.join(header, ",") + lineSeparator);
      writer.write(StringUtils.join(line1, ",") + lineSeparator);
      writer.write(StringUtils.join(line2, ",") + lineSeparator);
      writer.write(StringUtils.join(line3, ",") + lineSeparator);
    } catch (IOException e) {
      e.printStackTrace();
    }

    Map<Integer, List<Cell<String>>> data = new HashMap<>();
    data.put(
        1,
        IntStream.range(0, header.length)
            .mapToObj(index -> Cell.of(header[index], line1[index]))
            .collect(Collectors.toList()));
    data.put(
        2,
        IntStream.range(0, header.length)
            .mapToObj(index -> Cell.of(header[index], line2[index]))
            .collect(Collectors.toList()));
    data.put(
        3,
        IntStream.range(0, header.length)
            .mapToObj(index -> Cell.of(header[index], line3[index]))
            .collect(Collectors.toList()));

    return data;
  }

  @Test
  public void testTransform() {
    converter = createConverter();
    data = setupInputData();
    System.out.println(data);
    Map<Integer, Row> transformedData = converter.transform(data);
    System.out.println(transformedData);
    Assert.assertEquals(mapToRow(data), converter.transform(data));
  }

  @Test
  public void testTransform_WithFullSchema() {
    converter = createConverter(schema);
    data = setupInputData();
    Map<Integer, Row> transformedData = converter.transform(data);
    Assert.assertEquals(data.size(), transformedData.size());
    Assert.assertEquals(mapToRow(data), transformedData);
  }

  @Test
  public void testTransform_WithSingleColumn() {
    Column column = Column.builder().name("cf1").dataType(DataType.STRING).order(0).build();
    converter = createConverter(Arrays.asList(column));
    data = setupInputData();
    Map<Integer, Row> transformedData = converter.transform(data);
    Assert.assertEquals(data.size(), transformedData.size());
    transformedData
        .values()
        .forEach(
            row -> {
              Assert.assertEquals(1, row.size());
              Assert.assertEquals("a", row.cell(column.newName()).value());
            });
  }

  private Map<Integer, Row> mapToRow(Map<Integer, List<Cell<String>>> data) {
    return data.entrySet().stream()
        .collect(
            Collectors.toMap(
                e -> e.getKey(), e -> Row.of(e.getValue().stream().toArray(Cell[]::new))));
  }

  @Test
  public void testFindCellByName() {
    converter = createConverter();
    data = setupInputData();
    List<Cell<String>> cells = data.get(3);
    Cell<String> cell = converter.findCellByName(cells, "cf3");
    Assert.assertEquals("cf3", cell.name());
    Assert.assertEquals("c", cell.value());
  }

  @Test
  public void testConvertByType() {
    converter = createConverter();
    Assert.assertTrue(converter.convertByType("true", DataType.BOOLEAN) instanceof Boolean);
    Assert.assertTrue(converter.convertByType("127", DataType.BYTE) instanceof Byte);
    Assert.assertTrue(converter.convertByType("1", DataType.SHORT) instanceof Short);
    Assert.assertTrue(converter.convertByType("2", DataType.INT) instanceof Integer);
    Assert.assertTrue(converter.convertByType("3", DataType.LONG) instanceof Long);
    Assert.assertTrue(converter.convertByType("4", DataType.FLOAT) instanceof Float);
    Assert.assertTrue(converter.convertByType("5", DataType.DOUBLE) instanceof Double);
    Assert.assertTrue(converter.convertByType("str", DataType.STRING) instanceof String);
    Assert.assertTrue(converter.convertByType("obj", DataType.OBJECT) instanceof Object);
  }

  @Test(expected = NumberFormatException.class)
  public void testConvertByType_ThrowNumberFormatException() {
    converter = createConverter();
    converter.convertByType("128", DataType.BYTE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConvertByType_ThrowIllegalArgumentException() {
    converter = createConverter();
    converter.convertByType("row", DataType.ROW);
  }

  @Test
  public void testToRecords_BySingleRow() {
    converter = createConverter();
    data = setupInputData();
    Map<Integer, Row> rows = mapToRow(data);

    int index = 1;
    Row row = rows.get(index);
    List<RowSourceRecord> records = converter.toRecords(row, index);
    Assert.assertEquals(records.size(), topicNames.size());
    for (RowSourceRecord record : records) {
      Assert.assertTrue(topicNames.contains(record.topicName()));
      Assert.assertEquals(
          Collections.singletonMap(CsvSourceConverter.CSV_PARTITION_KEY, path),
          record.sourcePartition());
      Assert.assertEquals(
          Collections.singletonMap(CsvSourceConverter.CSV_OFFSET_KEY, index),
          record.sourceOffset());
      Assert.assertEquals(row, record.row());
    }
  }

  @Test
  public void testToRecords_ByManyRows() {
    converter = createConverter();
    data = setupInputData();
    Map<Integer, Row> rows = mapToRow(data);

    List<RowSourceRecord> records = converter.toRecords(rows);
    Assert.assertEquals(topicNames.size() * rows.size(), records.size());
  }

  @Test
  public void testToCells() throws IOException {
    converter = createConverter();
    data = setupInputData();
    InputStreamReader reader = new InputStreamReader(new FileInputStream(tempFile));
    Assert.assertEquals(data, converter.toCells(reader));
  }

  @Test
  public void testConvert() throws IOException {
    converter = createConverter();
    data = setupInputData();
    InputStreamReader reader = new InputStreamReader(new FileInputStream(tempFile));
    List<RowSourceRecord> records = converter.convert(reader);

    Assert.assertEquals(topicNames.size() * data.size(), records.size());
  }

  @Test
  public void testConvert_IfAllCached() throws IOException {
    converter =
        new CsvSourceConverter.Builder()
            .path(path)
            .topics(topicNames)
            .offsetCache(
                new OffsetCache() {
                  @Override
                  public void update(RowSourceContext context, String path) {}

                  @Override
                  public void update(String path, int index) {}

                  @Override
                  public boolean predicate(String path, int index) {
                    return false;
                  }
                })
            .schema(schema)
            .build();
    data = setupInputData();
    InputStreamReader reader = new InputStreamReader(new FileInputStream(tempFile));
    List<RowSourceRecord> records = converter.convert(reader);

    Assert.assertEquals(0, records.size());
  }

  @Test
  public void testRegex() {
    String[] splits = "1,\"2,3,4\",5".split(CsvSourceConverter.CSV_REGEX);
    Assert.assertEquals(3, splits.length);
    Assert.assertEquals("1", splits[0]);
    Assert.assertEquals("\"2,3,4\"", splits[1]);
    Assert.assertEquals("5", splits[2]);

    String[] splits2 = "1,3,5".split(CsvSourceConverter.CSV_REGEX);
    Assert.assertEquals(3, splits.length);
    Assert.assertEquals("1", splits2[0]);
    Assert.assertEquals("3", splits2[1]);
    Assert.assertEquals("5", splits2[2]);
  }

  class FakeOffsetCache implements OffsetCache {
    @Override
    public void update(RowSourceContext context, String path) {
      // DO NOTHING
    }

    @Override
    public void update(String path, int index) {
      // DO NOTHING
    }

    @Override
    public boolean predicate(String path, int index) {
      return true;
    }
  }
}
