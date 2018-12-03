package com.island.ohara.kafka.connector;

/**
 * use in RowSinkRecord
 *
 * @see RowSinkRecord
 */
public class TimestampType {
  private final int id;
  private final String name;

  public TimestampType(int id, String name) {
    this.id = id;
    this.name = name;
  }

  public int id() {
    return id;
  }

  public String name() {
    return name;
  }
}
