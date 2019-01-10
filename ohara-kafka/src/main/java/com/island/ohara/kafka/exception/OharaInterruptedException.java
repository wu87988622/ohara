package com.island.ohara.kafka.exception;

public class OharaInterruptedException extends OharaException {
  public OharaInterruptedException() {}

  public OharaInterruptedException(Throwable e) {
    super(e);
  }

  public OharaInterruptedException(String message, Throwable e) {
    super(message, e);
  }
}
