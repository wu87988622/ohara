package com.island.ohara.common.data.connector;

/** Kafka connector state */
public enum ConnectorState {
  UNASSIGNED,
  RUNNING,
  PAUSED,
  FAILED,
  DESTROYED
}
