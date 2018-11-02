package com.island.ohara.kafka
import java.util

import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.util.VersionUtil

import scala.collection.JavaConverters._
package object connector {
  private[this] val NAME_KEY: String = "name"
  private[this] val TOPICS_KEY: String = "topics"
  private[connector] def toTaskConfig(props: util.Map[String, String]): TaskConfig = {
    val options = props.asScala
    // TODO: the passed props is not a "copy" so any changes to props will impact props itself.
    // see OHARA-588 for more details...by chia
    val schema = options.get(Column.COLUMN_KEY).map(Column.toColumns).getOrElse(Seq.empty)
    val topics = options
      .get(TOPICS_KEY)
      .map(_.split(","))
      .getOrElse(throw new IllegalArgumentException(s"topics doesn't exist!!!"))
    // TODO: the passed props is not a "copy" so any changes to props will impact props itself.
    // see OHARA-588 for more details...by chia
    val name = options.getOrElse(NAME_KEY, throw new IllegalArgumentException(s"name doesn't exist!!!"))
    TaskConfig(name, topics, schema, options.toMap)
  }

  private[connector] def toMap(taskConfig: TaskConfig): util.Map[String, String] = {
    // TODO: the passed props is not a "copy" so any changes to props will impact props itself.
    // see OHARA-588 for more details...by chia
//    if (taskConfig.options.contains(Column.COLUMN_KEY))
//      throw new IllegalArgumentException(s"DON'T touch ${Column.COLUMN_KEY} manually")
//    if (taskConfig.options.contains(TOPICS_KEY))
//      throw new IllegalArgumentException(s"DON'T touch $TOPICS_KEY manually in row connector")
//    if (taskConfig.options.contains("name"))
//      throw new IllegalArgumentException("DON'T touch \"name\" manually in row connector")
    if (taskConfig.topics.isEmpty) throw new IllegalArgumentException("empty topics is invalid")
    (taskConfig.options + (Column.COLUMN_KEY -> Column.toString(taskConfig.schema))
      + (TOPICS_KEY -> taskConfig.topics.mkString(","))
      + (NAME_KEY -> taskConfig.name)).asJava
  }

  /**
    * this version is exposed to kafka connector. Kafka connector's version mechanism carry a string used to
    * represent the "version" only. It is a such weak function which can't carry other information - ex. revision.
    * Hence, we do a magic way to combine the revision with version and then parse it manually in order to provide
    * more powerful CLUSTER APIs (see ClusterRoute)
    */
  val VERSION: String = VersionUtil.VERSION + "_" + VersionUtil.REVISION
}
