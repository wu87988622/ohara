package com.island.ohara.connector.hdfs

import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.connector.hdfs.creator.StorageCreator
import com.island.ohara.connector.hdfs.storage.Storage
import com.island.ohara.kafka.connector.{RowSinkContext, RowSinkRecord, TopicPartition}

import scala.collection.mutable

/**
  * The partition added to TopicPartitionWriter collection to process data
  * @param config
  * @param context
  */
class DataWriter(config: HDFSSinkConnectorConfig, context: RowSinkContext, schema: Seq[Column]) {

  private[this] val createStorage: StorageCreator = Class
    .forName(config.hdfsStorageCreateClass)
    .getConstructor(classOf[HDFSSinkConnectorConfig])
    .newInstance(config)
    .asInstanceOf[StorageCreator]

  private[this] val storage: Storage = createStorage.storage()

  val topicPartitionWriters = new mutable.HashMap[TopicPartition, TopicPartitionWriter]()

  /**
    * Get the TopicPartition and added to TopicPartitionWriter collection
    * @param partitions
    */
  def createPartitionDataWriters(partitions: Seq[TopicPartition]): Unit = {
    partitions.foreach(partition => {
      topicPartitionWriters.put(partition, new TopicPartitionWriter(config, context, partition, storage))
    })

    //Check folder and recover partition offset
    topicPartitionWriters.values.foreach(_.open())
  }

  /**
    * Get topic data
    * @param records
    */
  def write(records: Seq[RowSinkRecord]): Unit = {
    records.foreach(record => {
      val topicName: String = record.topic
      val partition: Int = record.partition
      val oharaTopicPartition: TopicPartition = new TopicPartition(topicName, partition)
      topicPartitionWriters(oharaTopicPartition).write(schema, record)
    })

    //When topic data is empty for check the flush time to commit temp file to data dir.
    if (records.isEmpty)
      topicPartitionWriters.values.foreach(_.writer())
  }

  /**
    * close task
    * @param partitions
    */
  def removePartitionWriters(partitions: Seq[TopicPartition]): Unit = {
    partitions.foreach(partition => {
      val oharaTopicPartition: TopicPartition = new TopicPartition(partition.topic, partition.partition)
      topicPartitionWriters(oharaTopicPartition).close()
      topicPartitionWriters.remove(oharaTopicPartition)
    })
  }

  /**
    * Stop task and close FileSystem object
    */
  def stop(): Unit = {
    createStorage.close()
  }
}
