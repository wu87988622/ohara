package com.island.ohara.streams;

import com.island.ohara.common.data.Row;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class MyExtractor implements TimestampExtractor {

  private static DateTimeFormatter dataTimeFormatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Override
  public long extract(ConsumerRecord<Object, Object> record, long previousTimestamp) {
    Object value = record.value();
    if (value instanceof Row) {
      Row row = (Row) value;
      // orders
      if (row.names().contains("transactionDate"))
        return LocalDateTime.parse(
                    row.cell("transactionDate").value().toString(), dataTimeFormatter)
                .toEpochSecond(ZoneOffset.UTC)
            * 1000;
      // items
      if (row.names().contains("price"))
        return LocalDateTime.of(2015, 12, 11, 1, 0, 10).toEpochSecond(ZoneOffset.UTC) * 1000;
      // users
      else if (row.names().contains("gender"))
        return LocalDateTime.of(2015, 12, 11, 0, 0, 10).toEpochSecond(ZoneOffset.UTC) * 1000;
      // other
      else return LocalDateTime.of(2015, 12, 11, 2, 0, 10).toEpochSecond(ZoneOffset.UTC) * 1000;
    } else {
      return LocalDateTime.of(2015, 11, 10, 0, 0, 10).toEpochSecond(ZoneOffset.UTC) * 1000;
    }
  }
}
