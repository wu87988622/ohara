package com.island.ohara.kafka.exception;

public class OharaExecutionException extends OharaException {

  public OharaExecutionException() {}

  public OharaExecutionException(Throwable e) {
    super(e);
  }

  public OharaExecutionException(String message, Throwable e) {
    super(message, e);
  }
}
