# custom connector guideline

Ohara custom connector is based on [kafka connector](https://docs.confluent.io/current/connect/managing/index.html).
It offers a platform that enables you to define some simple actions to connect topic to any other system.
You don't need to worry the application availability, data durability, or distribution anymore. All you have to do is to
write your custom connector, which can have only the pull()/push() method, and then compile your code to a jar file.
After uploading your jar to ohara, you are able to **deploy** your  connector on the
[worker cluster](rest_interface.md#create-a-worker-cluster). By leveraging ohara connector framework, apart from the availability,
scalability, and durability, you can also monitor your connector for [logs](rest_interface.md#logs) and [metrics](#metrics). 

The following sections start to explain how to write a good connector on ohara. You don't need to read it through if you
are familiar with [kafka connector](https://docs.confluent.io/current/connect/managing/index.html). However, ohara connector has some
improvements which are not in [kafka connector](https://docs.confluent.io/current/connect/managing/index.html) so it has
worth of taking a look at [metrics](#metrics) and [setting definitions](#setting-definitions.)

----------

## ohara connector overview

Ohara connector is composed of [source connect](#source-connector) and [sink connector](#sink-connector). 
[source connect](#source-connector) is used to pull data **from external system to topic**. By contrast, [sink connector](#sink-connector)
is used to pull data from **topic to external system**. A complete connector consists of **SourceConnector**/
**SinkConnector** and **SourceTask**/**SinkTask**. Worker cluster picks up a node to host your source/sink
connector and then distributes the source/sink tasks across cluster. 

----------

### data model

Ohara has defined a table structure data in code base. We call it **row**. A row is comprised of multiple **cells**. Each cell has
its **name**, **value** and **tags**. The value in cell is a generic type which accepts any serializable type of value.
Following is an example that shows you how to convert a csv data to ohara row.

```
c0,c1,c2
v0,v1,v2
```

The above data can be converted to ohara row by following code.
```java
import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Cell;
class ExampleOfRow {
    public static void main(String[] args) {
        Row row = Row.of(
                Cell.of("c0", "v0"),
                Cell.of("c1", "v1"),
                Cell.of("c2", "v2")
                );
    }
}
```

```
c0,c1,c2
v0,,v2
```

The above data can be converted to ohara row by following code.
```java
import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Cell;
class ExampleOfRow {
    public static void main(String[] args) {
        Row row = Row.of(
                Cell.of("c0", "v0"),
                Cell.of("c2", "v2")
                );
    }
}
```

Don't worry about the serialization. Ohara offers default serializations for following data types.
1. string
1. boolean
1. short
1. int
1. long
1. float
1. double
1. bytes
1. serializable object
1. row (a nested row is acceptable!)

> The default serializer is located at com.island.ohara.common.data.Serializer.

When you get the rows in connector, you should follow the **cell setting** to generate the output.
The **cell setting** in ohara is called **column**. It shows the metadata of a **cell**. The metadata consists of
1. origin column name (**string**) — you can match the cell by this name
1. new column name — the new name of output.
1. type (**DataType**) — the type of output value. Whatever the origin type of value, you should convert the value according this type.
                        Don't worry the casting error. It is up to the user who pass the wrong configuration.
    - string
    - boolean
    - short
    - int
    - long
    - float
    - double
    - bytes
    - serializable object
    - row
1. order (**int**) — the order of cells in output.

An example of converting data according to columns.
```java
import com.island.ohara.common.data.Cell;
import com.island.ohara.common.data.Column;
class ExampleOfConverting {
    public static Object hello(Column column, String rawValue) {
        switch (column.dataType) {
            case DataType.BOOLEAN:
                return Boolean.valueOf(rawValue);
            case DataType.STRING:
                return rawValue;
            case DataType.SHORT:
                return Short.valueOf(rawValue);
            case DataType.INT:
                return Integer.valueOf(rawValue);
            case DataType.FLOAT:
                return Float.valueOf(rawValue);
            case DataType.DOUBLE:
                return Double.valueOf(rawValue);
            default:
                throw new IllegalArgumentException("unsupported type:" + column.dataType);
        }
    }
}
```

The type is a complicated issue since there are countless types in this world. It is impossible to define a general solution
to handle all types so the final types of value is **byte array** or **serializable object**. If the type you want to pass is not
in official support, you should define it as **byte array** or **serializable object** and then process it in your connectors.

> Feel free to throw an exception when your connector encounter a unknown type. Don't swallow it and convert to a weird value, such as null or empty.
Throwing exception is better than generating corrupt data! 

----------

### Source connector

Source connector is used to pull data from outside system and then push processed data to ohara topics. A basic implementation
for a source connector only includes four methods - _start, _stop, _taskClass, and _taskConfigs

```java
public abstract class RowSourceConnector extends SourceConnector {

  /**
   * Returns the RowSourceTask implementation for this Connector.
   *
   * @return a RowSourceTask class
   */
  protected abstract Class<? extends RowSourceTask> _taskClass();

  /**
   * Return the settings for source task.
   *
   * @param maxTasks number of tasks for this connector
   * @return a seq from settings
   */
  protected abstract List<TaskConfig> _taskConfigs(int maxTasks);

  /**
   * Start this Connector. This method will only be called on a clean Connector, i.e. it has either
   * just been instantiated and initialized or _stop() has been invoked.
   *
   * @param config configuration settings
   */
  protected abstract void _start(TaskConfig config);

  /** stop this connector */
  protected abstract void _stop();
}
```

> The methods having prefix "_" belong to ohara connector. Ohara connector is based on kafka connector. Ohara take
  control on all kafka APIs in order to supply more powerful and friendly APIs to ohara user. In order to distinguish the 
  APIs between ohara and kafka, we add prefix _ to all ohara methods and make them be abstract.  

----------

#### _start(TaskConfig)
After instantizing a connector, the first method called by worker is **start()**. You should initialize your connector
in **start** method, since it has a input parameter **TaskConfig** carrying all settings, such as target topics,
connector name and user-defined configs, from user. If you (connector developer) are a good friend of your connector user,
you can get (and cast it to expected type) config, which is passed by connector user, from **TaskConfig**. For example,
a connector user calls [Connector API](rest_interface.md#create-the-settings-of-connector) to store a config k0-v0 
(both of them are string type) for your connector, and then you can get v0 via TaskConfig.stringValue("k0").

> Don't be afraid of throwing exception when you notice that input parameters are incorrect. Throwing an exception can fail
  a connector quickly and stop worker to distribute connector task across cluster. It saves the time and resources. 

We all hate wrong configs, right? When you design the connector, you can **define** the [settings](#setting-definitions) on your
own initiative. The [settings](#setting-definitions) enable worker to check the input configs before starting connector.
It can't eliminate incorrect configs completely, but it save your time of fighting against wrong configs (have a great time with your family) 

----------

#### _stop()

This method is invoked by calling [STOP API](rest_interface.md#stop-a-connector). You can release the resources allocated by
connector, or send a email to shout at someone. It is ok to throw an exception when you fails to **stop** the connector.
Worker cluster will mark **failure** on the connector, and the world keeps running.

----------

#### _taskClass()

This method returns the java class of [RowSourceTask](#rowsourcetask) implementation. It tells worker cluster which class
should be created to pull data from external system. Noted that connector and task may not be created on same node (jvm)
so you should NOT share any objects between them (for example, make them to access a global variable).

----------

#### _taskConfigs(int maxTasks)

Connector has to generate configs for each task. The value of **maxTasks** is configured by [Connector API](rest_interface.md#connector).
If you prefer to make all tasks do identical job, you can just clone the task config passe by [start](#_starttaskconfig).
Or you can prepare different configs for each task. Noted that the number of configuration you return MUST be equal with
input value - maxTasks. Otherwise, you will get a exception when running your connector.

> It would be better to do the final check to input configs in Connector rather than Task. Producing a failure quickly
  save your time and resources.

----------

### RowSourceTask

```java
public abstract class RowSourceTask extends SourceTask {

  /**
   * Start the Task. This should handle any configuration parsing and one-time setup from the task.
   *
   * @param config initial configuration
   */
  protected abstract void _start(TaskConfig config);

  /**
   * Signal this SourceTask to stop. In SourceTasks, this method only needs to signal to the task
   * that it should stop trying to poll for new data and interrupt any outstanding poll() requests.
   * It is not required that the task has fully stopped. Note that this method necessarily may be
   * invoked from a different thread than _poll() and _commit()
   */
  protected abstract void _stop();
  /**
   * Poll this SourceTask for new records. This method should block if no data is currently
   * available.
   *
   * @return a array from RowSourceRecord
   */
  protected abstract List<RowSourceRecord> _poll();
}  
```

RowSourceTask is the unit of executing **poll**. A connector can invokes multiple tasks if you set **tasks.max** be bigger
than 1 via [Connector APIs](rest_interface.md#connector). RowSourceTask has similar lifecycle to Source connector. Worker
cluster call **start** to initialize a task and call **stop** to terminate a task.

----------

#### _pull()
You can ignore all methods except for **_poll**. Worker cluster call **_poll** regularly to get **RowSourceRecord**s and then
save them to topics. Worker cluster does not care for your implementation. All you have to do is to put your data in ***RowSourceRecord**.
RowSourceRecord is a complicated object having many elements. Some elements are significant. For example, **partition**
can impact the distribution of records. In order to be the best friend of programmer, ohara follows fluent pattern to allow
you to create record through builder, and you can only fill the required elements.
```java
public class ExampleOfRowSourceRecord {
    public static RowSourceRecord create(Row row, String topicName) {
        return RowSourceRecord.builder()
        .row(row)
        .topicName(topicName)
        .build();
    }
}
```

> You can read the java docs of RowSourceRecord.Builder to see which default values are set for other (optional) elements.

----------

#### partition and offsets

De-duplicating data is not a easy job. When you keep pulling data from external system to topics, you always need a place
to record which data have not processed. Connector offers two specific objects for you to record the **offset** and **partition**
of your data. You can define a **partition** and a **offset** for RowSourceRecord. The durability is on Worker's shoulder,
and you are always doable to get **partition** and **offset** back even if the connector fail or shutdown.
```java
public class ExampleOfRowSourceContext {
    public static Map<String, ?> getOffset(Map<String, ?> partition) {
        return RowSourceContext.offset(partition);
    }
}
```

Both of them are Map type with string key and primitive type. Using Map is a workaround to record the offsets for
different connectors. You can view them as a **flatten** json representation. For example, one of task is handling file_a,
and it has processed first line of file_a. Then the pair of **partition** and **offset** look like
```json
{
  "fileName": "file_a"
}
```
```json
{
  "offset": 1
}
```

We can convert above json to **partition** and **offset** and then put them in **RowSourceRecord**.

```java
public class ExampleOfPartitionAndOffset {
    public static RowSourceRecord addPartitionAndOffset(RowSourceRecord.Builder builder, String fileName, int offset) {
        Map<String, String> partition = Collections.singletonMap("fileName", fileName);
        Map<String, Integer> offset = Collections.singletonMap("offset", 1);
        return builder.sourcePartition(partition)
        .sourceOffset(offset)
        .build();
    }
}
```

A news of **partition** and **offset** is that they are not stored with data in RowSourceRecord. If you want to know the
commit of **partition** and **offset**, you can override the **_commit()**.
```java
public abstract class RowSourceTask extends SourceTask {
  /**
   * Commit the offsets, up to the offsets that have been returned by _poll(). This method should
   * block until the commit is complete.
   *
   * <p>SourceTasks are not required to implement this functionality; Kafka Connect will record
   * offsets automatically. This hook is provided for systems that also need to store offsets
   * internally in their own system.
   */
  protected void _commit() {
    // do nothing
  }
}
```

----------

#### handle exception in _poll()
Throwing exception make connector in **failure** state, and inactivate connector until you restart it. Hence, you SHOULD
catch and handle the exception as best you can. However, swallowing all exception is also a weired behavior. You SHOULD
fails the connector when encountering unrecoverable exception.

----------

#### blocking action is unwelcome in _poll()
Task is executed on a separate thread and there are many remaining processing for data after _poll(). Hence, you should 
NOT block _poll(). On the contrary, returning an empty list can yield the resource to remaining processing.

> Returning null results in same result. However, we all should hate null so please take away null from your code.

----------

#### data from _poll() are committed async
You don't expect that the data you generated are commit at once, right? Committing data invokes a large latency since
we need to sync data to multiple nodes and result in many disk I/O. Worker has another thread sending your data in background.
If your connector needs to know the time of committing data, you can override the **_commitRecord(RowSourceRecord)**.

```java
public abstract class RowSourceTask extends SourceTask {
  /**
   * Commit an individual RowSourceRecord when the callback from the producer client is received, or
   * if a record is filtered by a transformation. SourceTasks are not required to implement this
   * functionality; Kafka Connect will record offsets automatically. This hook is provided for
   * systems that also need to store offsets internally in their own system.
   *
   * @param record RowSourceRecord that was successfully sent via the producer.
   */
  protected void _commitRecord(RowSourceRecord record) {
    // do nothing
  }
}
```

----------

### Sink connector

```java
public abstract class RowSinkConnector extends SinkConnector {

  /**
   * Start this Connector. This method will only be called on a clean Connector, i.e. it has either
   * just been instantiated and initialized or _stop() has been invoked.
   *
   * @param config configuration settings
   */
  protected abstract void _start(TaskConfig config);

  /** stop this connector */
  protected abstract void _stop();

  /**
   * Returns the RowSinkTask implementation for this Connector.
   *
   * @return a RowSinkTask class
   */
  protected abstract Class<? extends RowSinkTask> _taskClass();

  /**
   * Return the settings for source task. NOTED: It is illegal to assign different topics to
   * RowSinkTask
   *
   * @param maxTasks number of tasks for this connector
   * @return the settings for each tasks
   */
  protected abstract List<TaskConfig> _taskConfigs(int maxTasks);
}
```

Sink connector is similar to [source connector](#source-connector). It also have [_start(TaskConfig)](#_starttaskconfig),
[_stop()](#_stop), [_taskClass()](#_taskclass), [_taskConfigs(int maxTasks)](#_taskconfigsint-maxtasks),
[partition and offsets](#partition-and-offsets). The main difference between sink connector and source connector is that
sink connector do pull data from topic and then push processed data to outside system. Hence, it does have
[_put](#_putlistrowsinkrecord-records) rather than [_pull](#_pull)

> Though sink connector and source connector have many identical methods, you should NOT make a connector mixed sink and source.
  Because Both connector are **abstract** class, you can't have a class extending both of them in java.

Sink connector also has to provide the task class to worker cluster. The sink task in ohara is called **RowSinkTask**.
It is also distributed across whole worker cluster when you running a sink connector. 

----------

### RowSinkTask

```java
public abstract class RowSinkTask extends SinkTask {

  /**
   * Start the Task. This should handle any configuration parsing and one-time setup from the task.
   *
   * @param config initial configuration
   */
  protected abstract void _start(TaskConfig config);

  /**
   * Perform any cleanup to stop this task. In SinkTasks, this method is invoked only once
   * outstanding calls to other methods have completed (e.g., _put() has returned) and a final
   * flush() and offset commit has completed. Implementations from this method should only need to
   * perform final cleanup operations, such as closing network connections to the sink system.
   */
  protected abstract void _stop();

  /**
   * Put the table record in the sink. Usually this should send the records to the sink
   * asynchronously and immediately return.
   *
   * @param records table record
   */
  protected abstract void _put(List<RowSinkRecord> records);
}  
```

RowSinkTask is similar to [RowSourceTask](#rowsourcetask) that both of them have _start and _stop phase. RowSinkTask is
executed by a separate thread on worker also.

----------

#### _put(List<RowSinkRecord> records)

Worker invokes a separate thread to fetch data from topic and put the data to sink task. The input data is called **RowSinkRecord**
which carries not only row but also metadata.
1. topicName (**string**) — where the dat come from
1. Row (**row**) — input data
1. partition (**int**) — index of partition
1. offset (**long**) — offset in topic-partition;
1. timestamp (**long**) — data timestamp
1. TimestampType (**enum**) — the way of generating timestamp
    - NO_TIMESTAMP_TYPE — means timestamp is nothing for this data
    - CREATE_TIME — the timestamp is provided by user or the time of sending this data 
    - LOG_APPEND_TIME — the timestamp is broker's local time when the data is append

----------

#### partition and offsets

Sink task has a component, which is called **RowSinkContext**, saving the offset and partitions for input data. Commonly,
it is not big news to you since kafka has responsibility to manage data offset in topic-partition to avoid losing data.
However, if you have something more than data lost, such as exactly once, you can manage the data offset manually and then
use RowSinkContext to change the offset of input data.


----------

#### handle exception in _put(List<RowSinkRecord>)
Any thrown exception will make this connector failed and stopped. You should handle the recoverable error and throw
the exception which obstruct connector from running. 

```java
public interface RowSinkContext {
  /**
   * Reset the consumer offsets for the given topic partitions. SinkTasks should use this if they
   * manage offsets in the sink data store rather than using Kafka consumer offsets. For example, an
   * HDFS connector might record offsets in HDFS to provide exactly once delivery. When the SinkTask
   * is started or a rebalance occurs, the task would reload offsets from HDFS and use this method
   * to reset the consumer to those offsets.
   *
   * <p>SinkTasks that do not manage their own offsets do not need to use this method.
   *
   * @param offsets map from offsets for topic partitions
   */
  void offset(Map<TopicPartition, Long> offsets);

  /**
   * Reset the consumer offsets for the given topic partition. SinkTasks should use if they manage
   * offsets in the sink data store rather than using Kafka consumer offsets. For example, an HDFS
   * connector might record offsets in HDFS to provide exactly once delivery. When the topic
   * partition is recovered the task would reload offsets from HDFS and use this method to reset the
   * consumer to the offset.
   *
   * <p>SinkTasks that do not manage their own offsets do not need to use this method.
   *
   * @param partition the topic partition to reset offset.
   * @param offset the offset to reset to.
   */
  default void offset(TopicPartition partition, Long offset) {
    this.offset(Collections.singletonMap(partition, offset));
  }
}
```

> Noted that data offset is a order in topic-partition so the input of RowSinkContext.offset consists of topic name
  and partition. 

----------

#### handle exception in _put(List<RowSinkRecord>)
see [handle exception in _poll()](#handle-exception-in-_poll)

----------

#### commit your output data when kafka commit input data

While feeding data into your sink task, kakfa also tries to commit previous data that make the data disappear from you.
The method **_preCommit** is a callback of committing data offset. If you want to manage the offsets, you can change what
to commit by kafka. Another use case is that you have some stuff which needs to be committed also, and you can trigger
the commit in this callback.

```java
public abstract class RowSinkTask extends SinkTask {
  /**
   * Pre-commit hook invoked prior to an offset commit.
   *
   * <p>The default implementation simply return the offsets and is thus able to assume all offsets
   * are safe to commit.
   *
   * @param offsets the current offset state as from the last call to _put, provided for convenience
   *     but could also be determined by tracking all offsets included in the RowSourceRecord's
   *     passed to _put.
   * @return an empty map if Connect-managed offset commit is not desired, otherwise a map from
   *     offsets by topic-partition that are safe to commit.
   */
  protected Map<TopicPartition, TopicOffset> _preCommit(Map<TopicPartition, TopicOffset> offsets) {
    return offsets;
  }
}  
```

> The offsets exceeding the latest consumed offset are discarded

----------

## version

We all love to show how good we are. If you are a connector designer, ohara connector offers a way to show the verion,
revision and author for a connector.

```java
public abstract class RowSourceConnector extends SourceConnector {
  /**
   * Get the version from this connector.
   *
   * @return the version, formatted as a String
   */
  protected ConnectorVersion _version() {
    return ConnectorVersion.builder().build();
  }
}
```

By default, all information in ConnectorVersion are **unknown**. You can override one of them or all of them when writing 
connector. The version information of a connector is showed by [Worker API](rest_interface.md#worker).

> Don't return null, please!!!

You can build a ConnectorVersion with fluent pattern.
```java
public class ExampleOfConnectorVersion {
  public static ConnectorVersion build() {
    return ConnectorVersion.builder()
              .version("my_version")
              .revision("my_revision")
              .author("my_user")
              .build();
  }
}
```

All official connectors have identical ConnectorVersion consisting of ohara's version, sha of commit and name of release manager.
Feel free to fill what you want in ConnectorVersion. For example, it is ok to leave **unknown** in **Author** if you 
are the people that we can't talk. However, Please don't use illegal values like **null** or **empty string**.

> Version in ohara connector is different to kafka connector. The later only supports **version** and it's APIs show only
  **version**. Hence, you can't get revision, author or other [settings](#setting-definitions) through kafka APIs 

----------

## metrics

----------

## setting definitions