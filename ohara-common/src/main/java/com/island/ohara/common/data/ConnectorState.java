package com.island.ohara.common.data;

/** Kafka connector state */
// TODO: we convert this to java only for testing in ohara-kafka...We should refactor the tests in
// order to move this back to scala... by chia
public enum ConnectorState {
  UNASSIGNED,
  RUNNING,
  PAUSED,
  FAILED,
  DESTROYED
}
