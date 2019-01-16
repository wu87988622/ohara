package com.island.ohara.streams.ostream;

import com.island.ohara.streams.data.Poneglyph;
import com.island.ohara.streams.data.Stele;
import com.island.ohara.streams.exception.CheckedExceptionUtil;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.kafka.common.utils.Utils;
import org.apache.kafka.streams.TopologyDescription;
import org.apache.kafka.streams.processor.internals.InternalTopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Topology implements AutoCloseable {

  private org.apache.kafka.streams.Topology topology;
  private org.apache.kafka.streams.KafkaStreams streams;

  private static final Logger log = LoggerFactory.getLogger(Topology.class);

  Topology(
      org.apache.kafka.streams.StreamsBuilder builder,
      org.apache.kafka.streams.StreamsConfig config,
      boolean isCleanStart,
      boolean describeOnly) {
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

    if (!describeOnly) {
      streams = new org.apache.kafka.streams.KafkaStreams(topology, config);
    }

    if (!describeOnly && isCleanStart) {
      // Delete the application's local state
      // only "action" functions will take effect
      streams.cleanUp();
    }
  }

  public String describe() {
    return topology.describe().toString();
  }

  public List<Poneglyph> getPoneglyphs() {
    return topology
        .describe()
        .subtopologies()
        .stream()
        .map(
            subtopology -> {
              Poneglyph pg = new Poneglyph();
              List<Stele> steles =
                  subtopology
                      .nodes()
                      .stream()
                      .map(
                          node -> {
                            String name =
                                (node instanceof InternalTopologyBuilder.Source)
                                    ? ((InternalTopologyBuilder.Source) node).topics()
                                    : ((node instanceof InternalTopologyBuilder.Sink)
                                        ? ((InternalTopologyBuilder.Sink) node).topic()
                                        : "");
                            return new Stele(
                                node.getClass().getSimpleName(),
                                node.name(),
                                name,
                                node.predecessors()
                                    .stream()
                                    .map(TopologyDescription.Node::name)
                                    .collect(Collectors.toList()),
                                node.successors()
                                    .stream()
                                    .map(TopologyDescription.Node::name)
                                    .collect(Collectors.toList()));
                          })
                      .collect(Collectors.toList());
              steles.forEach(stele -> pg.addStele(stele));
              return pg;
            })
        .collect(Collectors.toList());
  }

  public void start() {
    CheckedExceptionUtil.wrap(() -> streams.start());
  }

  @Override
  public void close() {
    streams.close();
  }
}
