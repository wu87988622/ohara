package com.island.ohara.kafka
import java.util

import com.island.ohara.client.ConfiguratorJson.Column
import scala.collection.JavaConverters._
package object connector {
  val TOPICS_KEY: String = "topics"
  private[connector] def toTaskConfig(props: util.Map[String, String]): TaskConfig = {
    val options = props.asScala
    val schema = Column.toColumns(
      options
        .remove(Column.COLUMN_KEY)
        .getOrElse(throw new IllegalArgumentException(s"${Column.COLUMN_KEY} doesn't exist!!!")))
    val topics = options
      .remove("topics")
      .map(_.split(","))
      .getOrElse(throw new IllegalArgumentException(s"topics doesn't exist!!!"))
    val name = options.remove("name").getOrElse(throw new IllegalArgumentException(s"name doesn't exist!!!"))
    TaskConfig(name, topics, schema, options.toMap)
  }

  private[connector] def toMap(taskConfig: TaskConfig): util.Map[String, String] = {
    if (taskConfig.options.contains(Column.COLUMN_KEY))
      throw new IllegalArgumentException(s"DON'T touch ${Column.COLUMN_KEY} manually")
    if (taskConfig.options.contains(TOPICS_KEY))
      throw new IllegalArgumentException(s"DON'T touch $TOPICS_KEY manually in row connector")
    if (taskConfig.options.contains("name"))
      throw new IllegalArgumentException("DON'T touch \"name\" manually in row connector")
    if (taskConfig.topics.isEmpty) throw new IllegalArgumentException("empty topics is invalid")
    (taskConfig.options + (Column.COLUMN_KEY -> Column.toString(taskConfig.schema))
      + (TOPICS_KEY -> taskConfig.topics.mkString(","))
      + ("name" -> taskConfig.name)).asJava
  }
}
