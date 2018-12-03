package com.island.ohara.kafka;

import java.util.Arrays;
import java.util.Objects;

public class Header {

  private final String key;
  private final byte[] value;

  public Header(String key, byte[] value) {
    this.key = key;
    this.value = value;
  }

  public String key() {
    return key;
  }

  public byte[] value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Header header = (Header) o;
    return Objects.equals(key, header.key) && Arrays.equals(value, header.value);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(key);
    result = 31 * result + Arrays.hashCode(value);
    return result;
  }
}
