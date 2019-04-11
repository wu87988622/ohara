# Ohara REST Interface

ohara provides a bunch of REST APIs to advanced user for managing data, applications and cluster.
Both request and response must have application/json content type, hence you should set content type to application/json in your request.
    
    Content-Type: application/json
    
and add content type of the response via the HTTP Accept header:

    Accept: application/json
    
----------
## Quick links to APIs
- [Version](#version)
- [Topic](#topic)
- [FTP Information](#ftp-connection-information)
- [HDFS Information](#hdfs-connection-information)
- [JDBC Information](#jdbc-connection-information)
- [Connector](#connector)
- [Pipeline](#pipeline)
- [Zookeeper](#zookeeper)
- [Broker](#broker)
- [Worker](#worker)
- [StreamApp](#streamapp)

----------
## object id
Most storable objects in ohara is assigned with a unique id. Most access to objects require the id to indicate which
object you want to request. You don't need to worry about how to generate the unique id but you need to **remember**
the id in order to operate the object. Or you can request **list** to see all objects and then fetch the id to to other
operation like delete and update.

----------
## Statuses & Errors

ohara leverages akka-http to support standards-compliant HTTP statuses. your clients should check the
HTTP status before parsing response entities. The error message in response body are format to json content.

```json
{
  "code": "java.lang.IllegalArgumentException",
  "message": "Unsupported restful api:vasdasd. Or the request is invalid to the vasdasd",
  "stack": "java.lang.IllegalArgumentException: Unsupported restful api:vasdasd. Or the request is invalid to the vasdasd at"
}
```
    
1. code (**string**) — the type of error. It is normally a type of java exception
1. message (**string**) — a brief description of error
1. stack (**string**) — error stack captured by server
----------
## Version

We all love to see the version of software, right? Ohara provide a API to display the details of version. It includes
following information.

1. version (**string**) -- version of configurator
1. user(**string**) -- builder of configurator
1. revision(**string**) -- latest commit of configurator
1. date(**string**) -- build date of configurator

----------
### get the version of ohara

*GET /v0/info*

**Example response**

```json
{
  "versionInfo": {
    "version": "0.3-SNAPSHOT",
    "user": "Chia-Ping Tsai",
    "revision": "9af9578041f069a9a452c7fda5f7ed7217c0deea",
    "date": "2019-03-21 17:55:06"
  }
}
```
----------
## Topic

Ohara topic is based on kafka topic. It means the creation of topic on ohara will invoke a creation of kafka also.
Also, the delete to ohara topic also invoke a delete request to kafka. The common properties in topic are shown below.

1. [id](#object-id) (**string**) — topic id
1. name (**string**) — topic name
1. brokerClusterName (**string**) — the broker cluster hosting this topic
1. numberOfReplications (**int**) — the number of replications for this topic
1. numberOfPartitions (**int**) — the number of partitions for this topic
1. lastModified (**long**) — the last time to update this topic

> Most properties are mapped to kafka. A little magic, but, is that ohara uses [id](#object-id) rather than "name" in naming kafka topic.
The reason is that kafka disallows us to change topic name after creation, and ohara UI (ohara-manager) supports user
to change "topic name" at runtime. Hence, we use a id instead of name specified by user to set kafka topic, and process
the name (passed from user) as a "label".

----------
### create a topic

*POST /v0/topics*

1. name (**string**) — topic name
1. brokerClusterName (optional **string**) — the broker cluster hosting this topic
(**If you don't specify the broker cluster in request, ohara will try to find a broker cluster for you.
And it works only if there is only a broker cluster exists in ohara**)
1. numberOfReplications (optional **int**) — the number of replications for this topic
(**it is illegal to input the number of replications which is larger than the number of broker nodes**)
1. numberOfPartitions (optional **int**)— the number of partitions for this topic

**Example request**

```json
{
  "name": "topic0",
  "numberOfReplications": 1,
  "numberOfPartitions": 1
}
```

**Example response**

```json
{
  "name": "topic0",
  "brokerClusterName": "preCreatedBkCluster",
  "lastModified": 1553498552595,
  "numberOfReplications": 1,
  "id": "715e09c9-b4ee-41cc-8d05-cb544904ac38",
  "numberOfPartitions": 1
}
```
----------
### update a topic

*POST /v0/topics/${id}*

1. name (**string**) — topic name
1. numberOfPartitions (**int**) — the number of partitions for this topic
(**it is illegal to decrease the number**)

**Example request**

```json
{
  "name": "topic0"
}
```

**Example response**

```json
{
  "name": "topic0",
  "brokerClusterName": "preCreatedBkCluster",
  "lastModified": 1553498552595,
  "numberOfReplications": 1,
  "id": "715e09c9-b4ee-41cc-8d05-cb544904ac38",
  "numberOfPartitions": 1
}
```
----------
### list all topics

*GET /v0/topics*

**Example response**

```json
[
  {
    "name": "topic0",
    "brokerClusterName": "preCreatedBkCluster",
    "lastModified": 1553498552595,
    "numberOfReplications": 1,
    "id": "715e09c9-b4ee-41cc-8d05-cb544904ac38",
    "numberOfPartitions": 1
  },
  {
    "name": "wk00",
    "brokerClusterName": "preCreatedBkCluster",
    "lastModified": 1553498375573,
    "numberOfReplications": 1,
    "id": "7903d57c-4e75-40a8-9f8f-42d59c76cfbb",
    "numberOfPartitions": 1
  }
]
```
----------
### delete a topic

*DELETE /v0/topics/${id}*

**Example response**

```json
{
  "name": "topic0",
  "brokerClusterName": "preCreatedBkCluster",
  "lastModified": 1553498552595,
  "numberOfReplications": 1,
  "id": "715e09c9-b4ee-41cc-8d05-cb544904ac38",
  "numberOfPartitions": 1
}
```
----------
### get a topic

*GET /v0/topics/${id}*

**Example response**

```json
{
  "name": "topic0",
  "brokerClusterName": "preCreatedBkCluster",
  "lastModified": 1553498552595,
  "numberOfReplications": 1,
  "id": "715e09c9-b4ee-41cc-8d05-cb544904ac38",
  "numberOfPartitions": 1
}
```
----------
## FTP Connection Information

You can store the ftp information in ohara if the data is used frequently. Currently, all data are stored by text. The
storable information is shown below.

1. name (**string**) — name of this ftp information
1. hostname (**string**) — ftp server hostname
1. port (**int**) — ftp server port
1. user (**string**) — account of ftp server
1. password (**string**) — password of ftp server

The following information are tagged by ohara.

1. [id](#object-id) (**string**) — ftp information id
1. lastModified (**long**) — the last time to update this ftp information
  
----------
### store a ftp information

*POST /v0/ftp*

1. name (**string**) — name of this ftp information
1. hostname (**string**) — ftp server hostname
1. port (**int**) — ftp server port
1. user (**string**) — account of ftp server
1. password (**string**) — password of ftp server

> the string value can't be empty or null. the port should be small than 65535 and larger than zero.

**Example request**

```json
{
  "name": "ftp0",
  "hostname": "node00",
  "port": 22,
  "user": "abc",
  "password": "pwd"
}
```

**Example response**

```json
{
  "name": "ftp0",
  "hostname": "node00",
  "port": 22,
  "user": "abc",
  "password": "pwd",
  "lastModified": 1553498552595,
  "id": "715e09c9-b4ee-41cc-8d05-cb544904ac38"
}
```
----------
### update a ftp information

*POST /v0/ftp/${id}*

1. name (**string**) — name of this ftp information
1. hostname (**string**) — ftp server hostname
1. port (**int**) — ftp server port
1. user (**string**) — account of ftp server
1. password (**string**) — password of ftp server

> the string value can't be empty or null. the port should be small than 65535 and larger than zero.

**Example request**

```json
{
  "name": "ftp0",
  "hostname": "node00",
  "port": 22,
  "user": "abc",
  "password": "pwd"
}
```

**Example response**

```json
{
  "name": "ftp0",
  "hostname": "node00",
  "port": 22,
  "user": "abc",
  "password": "pwd",
  "lastModified": 1553498552595,
  "id": "715e09c9-b4ee-41cc-8d05-cb544904ac38"
}
```
----------
### list all ftp information stored in ohara

*GET /v0/ftp*

**Example response**

```json
[
  {
    "name": "ftp0",
    "hostname": "node00",
    "port": 22,
    "user": "abc",
    "password": "pwd",
    "lastModified": 1553498552595,
    "id": "715e09c9-b4ee-41cc-8d05-cb544904ac38"
  }
]
```
----------
### delete a ftp information

*DELETE /v0/ftp/${id}*

**Example response**

```json
{
  "name": "ftp0",
  "hostname": "node00",
  "port": 22,
  "user": "abc",
  "password": "pwd",
  "lastModified": 1553498552595,
  "id": "715e09c9-b4ee-41cc-8d05-cb544904ac38"
}
```
----------
### get a ftp information

*GET /v0/ftp/${id}*

**Example response**

```json
{
  "name": "ftp0",
  "hostname": "node00",
  "port": 22,
  "user": "abc",
  "password": "pwd",
  "lastModified": 1553498552595,
  "id": "715e09c9-b4ee-41cc-8d05-cb544904ac38"
}
```
## HDFS Connection Information

Ohara supports to store the simple hdfs information which is running on single namenode without security configuration.

1. name (**string**) — name of this hdfs information.
1. uri (**string**) — hdfs connection information. The form looks like "hdfs://namenode:9999/"

The following information are tagged by ohara.

1. [id](#object-id) (**string**) — hdfs information id
1. lastModified (**long**) — the last time to update this hdfs information
----------
### store a hdfs information

*POST /v0/hdfs*

1. name (**string**) — name of this hdfs information.
1. uri (**string**) — hdfs connection information. The form looks like "hdfs://namenode:9999/"

**Example request**

```json
{
  "name": "hdfs0",
  "uri": "hdfs://namenode:9999"
}
```

**Example response**

```json
{
  "name": "hdfs0",
  "uri": "hdfs://namenode:9999",
  "lastModified": 1553498552595,
  "id": "715e09c9-b4ee-41cc-8d05-cb544904ac38"
}
```
----------
### update a hdfs information

*POST /v0/hdfs/${id}*

1. name (**string**) — name of this hdfs information.
1. uri (**string**) — hdfs connection information. The form looks like "hdfs://namenode:9999/"

**Example request**

```json
{
  "name": "hdfs0",
  "uri": "hdfs://namenode:9999"
}
```

**Example response**

```json
{
  "name": "hdfs0",
  "uri": "hdfs://namenode:9999",
  "lastModified": 1553498552595,
  "id": "715e09c9-b4ee-41cc-8d05-cb544904ac38"
}
```
----------
### list all hdfs information stored in ohara

*GET /v0/hdfs*

**Example response**

```json
[
  {
    "name": "hdfs0",
    "uri": "hdfs://namenode:9999",
    "lastModified": 1553498552595,
    "id": "715e09c9-b4ee-41cc-8d05-cb544904ac38"
  }
]
```
----------
### delete a hdfs information

*DELETE /v0/hdfs/${id}*

**Example response**

```json
{
  "name": "hdfs0",
  "uri": "hdfs://namenode:9999",
  "lastModified": 1553498552595,
  "id": "715e09c9-b4ee-41cc-8d05-cb544904ac38"
}
```
----------
### get a hdfs information

*GET /v0/hdfs/${id}*

**Example response**

```json
{
  "name": "hdfs0",
  "uri": "hdfs://namenode:9999",
  "lastModified": 1553498552595,
  "id": "715e09c9-b4ee-41cc-8d05-cb544904ac38"
}
```
----------
## JDBC Connection Information

Database is a common data source in our world. Ohara also supports to link database to be a part of streaming, so there
are also APIs which help us to store related information used to connect database. Given that we are in java world,
the jdbc is only supported now. The storable information is shown below.

1. name (**string**) — name of this jdbc information.
1. url (**string**) — jdbc connection information. format: jdbc:${database}://${serverName}\$instanceName:$portNumber
1. user (**string**) — the account which has permission to access database
1. password (**string**) — password of account. It is stored as text in ohara

The following information are tagged by ohara.

1. [id](#object-id) (**string**) — jdbc information id
1. lastModified (**long**) — the last time to update this jdbc information
----------
### store a jdbc information

*POST /v0/jdbc*

1. name (**string**) — name of this jdbc information.
1. url (**string**) — jdbc connection information. format: jdbc:${database}://${serverName}\$instanceName:$portNumber
1. user (**string**) — the account which has permission to access database
1. password (**string**) — password of account. It is stored as text in ohara

**Example request**

```json
{
  "name": "jdbc_name",
  "url": "jdbc:mysql",
  "user": "user",
  "password": "aaa"
}
```

**Example response**

```json
{
  "name": "jdbc_name",
  "url": "jdbc:mysql",
  "lastModified": 1540967970407,
  "id": "9d128f43-8725-42b2-9377-0dad10863166",
  "user": "user",
  "password": "aaa"
}
```
----------
### update a jdbc information

*POST /v0/jdbc/${id}*

1. name (**string**) — name of this jdbc information.
1. url (**string**) — jdbc connection information. format: jdbc:${database}://${serverName}\$instanceName:$portNumber
1. user (**string**) — the account which has permission to access database
1. password (**string**) — password of account. It is stored as text in ohara

**Example request**

```json
{
  "name": "jdbc_name",
  "url": "jdbc:mysql",
  "user": "user",
  "password": "aaa"
}
```

**Example response**

```json
{
  "name": "jdbc_name",
  "url": "jdbc:mysql",
  "lastModified": 1540967970407,
  "id": "9d128f43-8725-42b2-9377-0dad10863166",
  "user": "user",
  "password": "aaa"
}
```
----------
### list all jdbc information stored in ohara

*GET /v0/jdbc*

**Example response**

```json
[
  {
    "name": "jdbc_name",
    "url": "jdbc:mysql",
    "lastModified": 1540967970407,
    "id": "9d128f43-8725-42b2-9377-0dad10863166",
    "user": "user",
    "password": "aaa"
  }
]
```
----------
### delete a jdbc information

*DELETE /v0/jdbc/${id}*

**Example response**

```json
{
  "name": "jdbc_name",
  "url": "jdbc:mysql",
  "lastModified": 1540967970407,
  "id": "9d128f43-8725-42b2-9377-0dad10863166",
  "user": "user",
  "password": "aaa"
}
```
----------
### get a jdbc information

*GET /v0/jdbc/${id}*

**Example response**

```json
{
  "name": "jdbc_name",
  "url": "jdbc:mysql",
  "lastModified": 1540967970407,
  "id": "9d128f43-8725-42b2-9377-0dad10863166",
  "user": "user",
  "password": "aaa"
}
```
----------
## Connector

Connector is core of application in ohara [pipeline](#pipeline). Connector has two type - source and sink. Source connector
pulls data from another system and then push to topic. By contrast, Sink connector pulls data from topic and then push to
another system. In order to use connector in [pipeline](#pipeline), you have to set up a connector settings in ohara and then add it
to [pipeline](#pipeline). Of course, the connector settings must belong to a existent connector in target worker cluster. By default,
worker cluster hosts only the official connectors. If you have more custom requirement for connector, please follow
[custome connector guideline](custom_connector.md) to write your connector. 

Apart from custom settings, common settings are required by all connectors. The common settings are shown below.
1. connector.name (**string**) — the name of this connector
1. connector.class (**class**) — class name of connector implementation
1. topics(**array(string)**) — the source topics or target topics for this connector
1. columns (**array(object)**) — the schema of data for this connector
  - columns[i].name (**string**) — origin name of column
  - columns[i].newName (**string**) - new name of column
  - columns[i].dataType (**string**) - the type used to convert data
  - columns[i].order (**int**) - the order of this column
1. numberOfTasks (**int**) — the number of tasks
1. workerClusterName (**string**) - target worker cluster

The following information are updated by ohara.
1. [id](#object-id) (**string**) — connector's id
1. lastModified (**long**) — the last time to update this connector
1. state (**string**) — the state of a started connector. If the connector is not started, you won't see this field
1. error (**string**) — the error message from a failed connector. If the connector is fine or un-started, you won't get this field.
1. [metrics](custom_connector.md#metrics) (**object**) — the metrics from a running connector
  - counters (**array(object)**) — the metrics in counter type
    - counters[i].value (**long**) — the number stored in counter
    - counters[i].unit (**string**) — unit for value
    - counters[i].document (**string**) — document of this counter
    - counters[i].startTime (**long**) — start time of counter (Normally, it is equal to create time)
    
The settings from request, BTW, is a individual item in response. Hence, you will observe the following response
after you store the settings with connector.class.

```json
{
  "settings": {
    "connector.class": "abc"
  }
}

```
----------
### create the settings of connector

*POST /v0/connectors*

It is ok to lack some common settings when creating settings for a connector. However, it is illegal to start a connector
with incomplete settings. For example, storing the settings consisting of only **connector.name** is ok. But stating
a connector with above incomplete settings will introduce a error.

**Example request**

```json
{
  "connector.name": "jdbc_name",
  "connector.class": "com.island.ohara.connector.ftp.FtpSource"
}
```

**Example response**

```json
{
  "lastModified": 1540967970407,
  "id": "9d128f43-8725-42b2-9377-0dad10863166",
  "settings": {
    "connector.name": "jdbc_name",
    "connector.class": "com.island.ohara.connector.ftp.FtpSource"
  },
  "metrics": {
    "counters": []
  }
}
```
----------
### update the settings of connector

*POST /v0/connectors/${id}*

**Example request**

```json
{
  "connector.name": "jdbc_name",
  "connector.class": "com.island.ohara.connector.ftp.FtpSource"
}
```

**Example response**

```json
{
  "lastModified": 1540967970407,
  "id": "9d128f43-8725-42b2-9377-0dad10863166",
  "settings": {
    "connector.name": "jdbc_name",
    "connector.class": "com.island.ohara.connector.ftp.FtpSource"
  },
  "metrics": {
    "counters": []
  }
}
```
----------
### list information of all connectors

*GET /v0/connectors*

**Example response**

```json
[
  {
    "lastModified": 1540967970407,
    "id": "9d128f43-8725-42b2-9377-0dad10863166",
    "settings": {
      "connector.name": "jdbc_name",
      "connector.class": "com.island.ohara.connector.ftp.FtpSource"
    },
    "metrics": {
      "counters": []
    }
  }
]
```
----------
### delete a connector

*DELETE /v0/connectors/${id}*

Deleting the settings used by a running connector is not allowed. You should [stop](#stop-a-connector) connector before deleting it.

**Example response**

```json
{
  "lastModified": 1540967970407,
  "id": "9d128f43-8725-42b2-9377-0dad10863166",
  "settings": {
    "connector.name": "jdbc_name",
    "connector.class": "com.island.ohara.connector.ftp.FtpSource"
  },
  "metrics": {
    "counters": []
  }
}
```
----------
### get information of connector

*GET /v0/connectors/${id}*

**Example response**

```json
{
  "lastModified": 1540967970407,
  "id": "9d128f43-8725-42b2-9377-0dad10863166",
  "settings": {
    "connector.name": "jdbc_name",
    "connector.class": "com.island.ohara.connector.ftp.FtpSource"
  },
  "metrics": {
    "counters": []
  }
}
```
----------
### start a connector

*PUT /v0/connectors/${id}/start*

Ohara will send a start request to specific worker cluster to start the connector with stored settings, and then make
a response to called. The connector is executed async so the connector may be still in starting after you retrieve
the response. You can send [GET request](#get-information-of-connector) to see the state of connector.
This request is idempotent so it is safe to retry this command repeatedly.

**Example response**

```json
{
  "lastModified": 1540967970407,
  "id": "9d128f43-8725-42b2-9377-0dad10863166",
  "settings": {
    "connector.name": "jdbc_name",
    "connector.class": "com.island.ohara.connector.ftp.FtpSource"
  },
  "state": "RUNNING",
  "metrics": {
    "counters": [
      {
        "value": 1234,
        "unit": "rows",
        "document": "number of processed rows",
        "startTime": 111111
      }
    ]
  }
}
```
### stop a connector

*PUT /v0/connectors/${id}/stop*

Ohara will send a stop request to specific worker cluster to stop the connector. The stopped connector will be removed from
worker cluster. The settings of connector is still kept by ohara so you can start the connector with same settings again
in the future. If you want to delete the connector totally, you should stop the connector and then [delete](#delete-a-connector) it.
This request is idempotent so it is safe to send this request repeatedly.

**Example response**

```json
{
  "lastModified": 1540967970407,
  "id": "9d128f43-8725-42b2-9377-0dad10863166",
  "settings": {
    "connector.name": "jdbc_name",
    "connector.class": "com.island.ohara.connector.ftp.FtpSource"
  },
  "metrics": {
    "counters": []
  }
}
```
----------
### pause a connector

*PUT /v0/connectors/${id}/pause*

Pausing a connector is to disable connector to pull/push data from/to source/sink. The connector is still alive in kafka.
This request is idempotent so it is safe to send this request repeatedly.

**Example response**

```json
{
  "lastModified": 1540967970407,
  "id": "9d128f43-8725-42b2-9377-0dad10863166",
  "settings": {
    "connector.name": "jdbc_name",
    "connector.class": "com.island.ohara.connector.ftp.FtpSource"
  },
  "state": "PAUSED",
  "metrics": {
    "counters": [
      {
        "value": 1234,
        "unit": "rows",
        "document": "number of processed rows",
        "startTime": 111111
      }
    ]
  }
}
```
----------
### resume a connector

*PUT /v0/connectors/${id}/resume*

Resuming a connector is to enable connector to pull/push data from/to source/sink.
This request is idempotent so it is safe to retry this command repeatedly.

**Example response**

```json
{
  "lastModified": 1540967970407,
  "id": "9d128f43-8725-42b2-9377-0dad10863166",
  "settings": {
    "connector.name": "jdbc_name",
    "connector.class": "com.island.ohara.connector.ftp.FtpSource"
  },
  "state": "RUNNING",
  "metrics": {
    "counters": [
      {
        "value": 1234,
        "unit": "rows",
        "document": "number of processed rows",
        "startTime": 111111
      }
    ]
  }
}
```
----------
## Pipeline

Pipeline APIs are born of ohara-manager which needs a way to store the relationship of components in streaming.
The relationship in pipeline is made up of multi **flows**. Each **flow** describe a **from** and multi **to**s.
For example, you have a [topic](#topic) as source and a [connector](#connector) as consumer, so you can describe the relationship
via following flow.
```json
{
  "flows": [
    {
      "from": "topic's id",
      "to": ["connector's id"]
    }
  ]
}
```

Each object described in pipeline must be related to a existent object. If you delete a object which is already used by
a pipeline, it will be deleted from pipeline's flow.

All objects in pipeline MUST work on the same cluster hierarchy. For example, you have following clusters.
1. a ([worker cluster](#worker)) based on
1. b ([broker cluster](#broker)) based on
1. c ([zookeeper cluster](#zookeeper))

When you add a [topic](#topic) to pipeline which is located at cluster-a, the [topic](#topic) must be located at broker-b. Otherwise, you will get
a error saying the [topic](#topic) can't be belong to you. A [connect](#connector) which is not hosted by same worker cluster
can't be related to pipeline also.

The properties used in generating pipeline are shown below.
1. name (**string**) — pipeline's name
1. flows (**array(object)**) — the relationship between objects
  - flows[i].from (**string**) — the endpoint of source
  - flows[i].to (**array(string)**) — the endpoint of sink
1. workerClusterName (**string**) - target worker cluster
  
Following information are written by ohara.
 1. [id](#object-id) (**string**) — pipeline's id
 1. lastModified (**long**) — the last time to update this pipeline
 1. objects(**array(object)**) — the abstract of all objects mentioned by pipeline
   - objects[i].[id](#object-id) (**string**) — object's id
   - objects[i].name (**string**) — object's name
   - objects[i].kind (**string**) — the type of this object. for instance, [topic](#topic), [connector](#connector), and [streamapp](#streamapp) 
   - objects[i].className (**string**) — object's implementation. Normally, it shows the full name of a java class
   - objects[i].state (**string**) — the state of object. If the object can't have state (eg, [topic](#topic)), you won't see this field
   - objects[i].error (**string**) — the error message of this object
   - objects[i].lastModified (**long**) — the last time to update this object
   - [metrics](custom_connector.md#metrics) (**object**) — the metrics from this object. Not all objects in pipeline have metrics!
     - counters (**array(object)**) — the metrics in counter type
       - counters[i].value (**long**) — the number stored in counter
       - counters[i].unit (**string**) — unit for value
       - counters[i].document (**string**) — document of this counter
       - counters[i].startTime (**long**) — start time of counter (Normally, it is equal to create time)
----------
### create a pipeline

*POST /v0/pipelines*

The following example creates a pipeline with a [topic](#topic) and [connector](#connector). The [topic](#topic) is created on
[broker cluster](#broker) but the [connector](#connector) isn't. Hence, the response from server shows that it fails
to find the status of the [connector](#connector). That is to say, it is ok to add un-running [connector](#connector) to pipeline.

**Example request 1**

```json
{
  "name": "pipeline0",
  "flows": [
    {
      "from": "be48b7d8-08a8-40a4-8f17-aaa",
      "to": ["81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"]
    }
  ]
}
```

**Example response 1**

```json
{
  "name": "pipeline0",
  "lastModified": 1554950999668,
  "workerClusterName": "wk00",
  "flows": [
    {
      "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
      "to": [
        "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"
      ]
    }
  ],
  "id": "e77e7c3e-1b73-4d31-ad85-ff575f0850f2",
  "objects": [
    {
      "name": "topic0",
      "lastModified": 1554950034608,
      "id": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
      "metrics": {
        "counters": []
      },
      "kind": "topic"
    },
    {
      "name": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
      "lastModified": 1554950058696,
      "id": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
      "error": "Failed to get status and type of connector:81cb80a9-34a5-4e45-881a-cb87d4fbb5bd.This may be temporary since our worker cluster is too busy to sync status of connector. abc doesn't exist",
      "metrics": {
        "counters": []
      },
      "kind": "connector"
    }
  ]
}
```

Don't worry about creating a pipeline with incomplete flows. It is ok to add a flow with only **from**. The following
example creates a pipeline with only a object and leave empty in **to** field.

**Example request 1**

```json
{
  "name": "pipeline1",
  "flows": [
    {
      "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
      "to": []
    }
  ]
}
```

**Example response 1**

```json
{
  "name": "pipeline1",
  "lastModified": 1554952500972,
  "workerClusterName": "wk00",
  "flows": [
    {
      "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
      "to": []
    }
  ],
  "id": "8105c8cd-7e75-46a6-9142-65afde430b2d",
  "objects": [
    {
      "name": "topic0",
      "lastModified": 1554950034608,
      "id": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
      "metrics": {
        "counters": []
      },
      "kind": "topic"
    }
  ]
}
```
----------
### update a pipeline

*POST /v0/pipelines/$id*

**Example request**

```json
{
  "name": "pipeline0",
  "flows": [
    {
      "from": "be48b7d8-08a8-40a4-8f17-aaa",
      "to": ["81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"]
    }
  ]
}
```

**Example response**

```json
{
  "name": "pipeline0",
  "lastModified": 1554950999668,
  "workerClusterName": "wk00",
  "flows": [
    {
      "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
      "to": [
        "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"
      ]
    }
  ],
  "id": "e77e7c3e-1b73-4d31-ad85-ff575f0850f2",
  "objects": [
    {
      "name": "topic0",
      "lastModified": 1554950034608,
      "id": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
      "metrics": {
        "counters": []
      },
      "kind": "topic"
    },
    {
      "name": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
      "lastModified": 1554950058696,
      "id": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
      "error": "Failed to get status and type of connector:81cb80a9-34a5-4e45-881a-cb87d4fbb5bd.This may be temporary since our worker cluster is too busy to sync status of connector. abc doesn't exist",
      "metrics": {
        "counters": []
      },
      "kind": "connector"
    }
  ]
}
```
----------
### list all pipelines

*GET /v0/pipelines*

Listing all pipelines is a expensive operation as it invokes a iteration to all objects stored in pipeline. The loop will
do a lot of checks and fetch status, metrics and log from backend clusters. If you have the id of pipeline, please
use [GET](#get-a-pipeline) to fetch details of **single** pipeline.

**Example response**

```json
[
  {
    "name": "pipeline0",
    "lastModified": 1554950999668,
    "workerClusterName": "wk00",
    "flows": [
      {
        "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
        "to": [
          "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"
        ]
      }
    ],
    "id": "e77e7c3e-1b73-4d31-ad85-ff575f0850f2",
    "objects": [
      {
        "name": "topic0",
        "lastModified": 1554950034608,
        "id": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
        "metrics": {
          "counters": []
        },
        "kind": "topic"
      },
      {
        "name": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
        "lastModified": 1554950058696,
        "id": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
        "error": "Failed to get status and type of connector:81cb80a9-34a5-4e45-881a-cb87d4fbb5bd.This may be temporary since our worker cluster is too busy to sync status of connector. abc doesn't exist",
        "metrics": {
          "counters": []
        },
        "kind": "connector"
      }
    ]
  }
]
```
----------
### delete a pipeline

*DELETE /v0/pipelines/$id*

Deleting a pipeline does not delete the objects related to the pipeline.

**Example response**

```json
{
  "name": "pipeline0",
  "lastModified": 1554950999668,
  "workerClusterName": "wk00",
  "flows": [
    {
      "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
      "to": [
        "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"
      ]
    }
  ],
  "id": "e77e7c3e-1b73-4d31-ad85-ff575f0850f2",
  "objects": [
    {
      "name": "topic0",
      "lastModified": 1554950034608,
      "id": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
      "metrics": {
        "counters": []
      },
      "kind": "topic"
    },
    {
      "name": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
      "lastModified": 1554950058696,
      "id": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
      "error": "Failed to get status and type of connector:81cb80a9-34a5-4e45-881a-cb87d4fbb5bd.This may be temporary since our worker cluster is too busy to sync status of connector. abc doesn't exist",
      "metrics": {
        "counters": []
      },
      "kind": "connector"
    }
  ]
}
```
----------
### get a pipeline

*GET /v0/pipelines/$id*

**Example response**

```json
{
  "name": "pipeline0",
  "lastModified": 1554950999668,
  "workerClusterName": "wk00",
  "flows": [
    {
      "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
      "to": [
        "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"
      ]
    }
  ],
  "id": "e77e7c3e-1b73-4d31-ad85-ff575f0850f2",
  "objects": [
    {
      "name": "topic0",
      "lastModified": 1554950034608,
      "id": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
      "metrics": {
        "counters": []
      },
      "kind": "topic"
    },
    {
      "name": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
      "lastModified": 1554950058696,
      "id": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
      "error": "Failed to get status and type of connector:81cb80a9-34a5-4e45-881a-cb87d4fbb5bd.This may be temporary since our worker cluster is too busy to sync status of connector. abc doesn't exist",
      "metrics": {
        "counters": []
      },
      "kind": "connector"
    }
  ]
}
```
----------
## zookeeper

----------
## broker

----------
## worker

----------
## streamapp

----------