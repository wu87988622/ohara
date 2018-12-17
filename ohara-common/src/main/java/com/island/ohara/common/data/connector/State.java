package com.island.ohara.common.data.connector;

/** Kafka connector state */
public enum State {
  UNASSIGNED,
  RUNNING,
  PAUSED,
  FAILED,
  DESTROYED
}
