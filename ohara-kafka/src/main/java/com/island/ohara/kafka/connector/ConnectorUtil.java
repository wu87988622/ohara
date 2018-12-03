package com.island.ohara.kafka.connector;

import com.island.ohara.client.ConfiguratorJson;
import com.island.ohara.client.ConfiguratorJson.Column$;
import com.island.ohara.common.util.VersionUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// TODO: 2018/11/15  Using scala Column

// package object connector
public class ConnectorUtil {

  private static final String NAME_KEY = "name";
  private static final String TOPICS_KEY = "topics";

  static TaskConfig toTaskConfig(Map<String, String> props) {
    Column$.MODULE$.toColumns(props.get(Column$.MODULE$.COLUMN_KEY()));

    // TODO: the passed props is not a "copy" so any changes to props will impact props itself.
    // see OHARA-588 for more details...by chia
    java.util.List<ConfiguratorJson.Column> schema =
        scala.collection.JavaConversions.seqAsJavaList(
            Column$.MODULE$.toColumns(props.get(Column$.MODULE$.COLUMN_KEY())));

    java.util.List<String> topics =
        Optional.ofNullable(props.get(TOPICS_KEY))
            .map((x) -> x.split(","))
            .map(Arrays::asList)
            .orElseThrow(() -> new IllegalArgumentException("topics doesn't exist!!!"));

    // TODO: the passed props is not a "copy" so any changes to props will impact props itself.
    // see OHARA-588 for more details...by chia
    String name =
        Optional.ofNullable(props.get(NAME_KEY))
            .orElseThrow(() -> new IllegalArgumentException("name doesn't exist!!!"));

    return new TaskConfig(name, topics, schema, props);
  }

  static Map<String, String> toMap(TaskConfig taskConfig) {
    // TODO: the passed props is not a "copy" so any changes to props will impact props itself.
    // see OHARA-588 for more details...by chia
    //    if (taskConfig.options.contains(Column.COLUMN_KEY))
    //      throw new IllegalArgumentException(s"DON'T touch ${Column.COLUMN_KEY} manually")
    //    if (taskConfig.options.contains(TOPICS_KEY))
    //      throw new IllegalArgumentException(s"DON'T touch $TOPICS_KEY manually in row connector")
    //    if (taskConfig.options.contains("name"))
    //      throw new IllegalArgumentException("DON'T touch \"name\" manually in row connector")
    if (taskConfig.topics().isEmpty())
      throw new IllegalArgumentException("empty topics is invalid");
    Map<String, String> map = new HashMap<>();
    map.putAll(taskConfig.options());

    map.put(
        Column$.MODULE$.COLUMN_KEY(),
        Column$.MODULE$.fromColumns(
            scala.collection.JavaConversions.asScalaBuffer(taskConfig.schema())));
    map.put(TOPICS_KEY, taskConfig.topics().stream().collect(Collectors.joining(",")));
    map.put(NAME_KEY, taskConfig.name());
    return map;
  }
  /**
   * this version is exposed to kafka connector. Kafka connector's version mechanism carry a string
   * used to represent the "version" only. It is a such weak function which can't carry other
   * information - ex. revision. Hence, we do a magic way to combine the revision with version and
   * then parse it manually in order to provide more powerful CLUSTER APIs (see ClusterRoute)
   */
  static final String VERSION = VersionUtil.VERSION + "_" + VersionUtil.REVISION;

  public static String VERSION() {
    return VERSION;
  }
}
