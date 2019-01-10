package com.island.ohara.kafka.exception;

public class OharaTimeoutException extends OharaException {
  public OharaTimeoutException() {}

  public OharaTimeoutException(Throwable e) {
    super(e);
  }

  public OharaTimeoutException(String message, Throwable e) {
    super(message, e);
  }
}
