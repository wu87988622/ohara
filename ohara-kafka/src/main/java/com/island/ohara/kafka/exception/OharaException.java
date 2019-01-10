package com.island.ohara.kafka.exception;

public class OharaException extends RuntimeException {

  public OharaException() {}

  public OharaException(String message) {
    super(message);
  }

  public OharaException(Throwable e) {
    super(e);
  }

  public OharaException(String message, Throwable e) {
    super(message, e);
  }
}
