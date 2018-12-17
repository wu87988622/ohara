package com.island.ohara.kafka.connector;

import com.island.ohara.common.rule.SmallTest;
import java.util.Collections;
import org.junit.Test;

public class TestConnectorProps extends SmallTest {

  @Test
  public void testNoTopicsInSourceConnector() {
    DumbSource connector = new DumbSource();
    // lack topics string
    assertException(
        IllegalArgumentException.class,
        () -> {
          connector.start(Collections.emptyMap());
        });
  }

  @Test
  public void testNoTopicsInSinkConnector() {
    DumbSink connector = new DumbSink();
    // lack topics string
    assertException(
        IllegalArgumentException.class,
        () -> {
          connector.start(Collections.emptyMap());
        });
  }

  @Test
  public void testNoTopicsInSourceTask() {
    DumbSourceTask task = new DumbSourceTask();
    assertException(
        IllegalArgumentException.class,
        () -> {
          task.start(Collections.emptyMap());
        });
  }

  @Test
  public void testNoTopicsInSinkTask() {
    DumbSinkTask task = new DumbSinkTask();
    assertException(
        IllegalArgumentException.class,
        () -> {
          task.start(Collections.emptyMap());
        });
  }

  // TODO: add tests against adding interval key manually...see OHARA-588
}
