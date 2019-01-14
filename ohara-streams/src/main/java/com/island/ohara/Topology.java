package com.island.ohara;

import com.island.ohara.exception.CheckedExceptionUtil;
import java.io.File;
import java.io.IOException;
import org.apache.kafka.common.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Topology implements AutoCloseable {

  private org.apache.kafka.streams.Topology topology;
  private org.apache.kafka.streams.KafkaStreams streams;

  private static final Logger log = LoggerFactory.getLogger(Topology.class);

  Topology(
      org.apache.kafka.streams.StreamsBuilder builder,
      org.apache.kafka.streams.StreamsConfig config,
      boolean isCleanStart) {
    this.topology = builder.build();

    // For now, windows handle cleanUp() -> DeleteFile(lock) with different behavior as Linux and
    // MacOS
    // We need to "directly" delete the state.dir instead of calling streams.cleanUp()
    // until the following JIRA fixed
    // See : https://issues.apache.org/jira/browse/KAFKA-6647
    if (isCleanStart) {
      final File baseDir =
          new File(config.getString(org.apache.kafka.streams.StreamsConfig.STATE_DIR_CONFIG));
      final File stateDir =
          new File(
              baseDir,
              config.getString(org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG));
      try {
        Utils.delete(stateDir);
      } catch (IOException e) {
        log.error("CleanUp state.dir failed!", e);
      }
    }

    streams = new org.apache.kafka.streams.KafkaStreams(topology, config);

    if (isCleanStart) {
      // Delete the application's local state
      streams.cleanUp();
    }
  }

  public String describe() {
    topology
        .describe()
        .subtopologies()
        .forEach(
            subtopology -> {
              subtopology
                  .nodes()
                  .forEach(
                      node -> {
                        // determine node type : source ; processor ; sink
                        //                System.out.println(node.getClass().getName());
                        // link graph : predecessor -- node -- successor
                        //                node.successors(); node.predecessors();
                      });
            });

    return topology.describe().toString();
  }

  public void start() {
    CheckedExceptionUtil.wrap(() -> streams.start());
  }

  @Override
  public void close() {
    streams.close();
  }
}
