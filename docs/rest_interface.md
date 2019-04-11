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
1. [id](#object-id) (**string**) — jdbc information id
1. lastModified (**long**) — the last time to update this jdbc information
1. state (**string**) — the state of a started connector. If the connector is not started, you won't see this field
1. error (**string**) — the error message from a failed connector. If the connector is fine or un-started, you won't get this field.
1. metrics (**object**) — the metrics from a running connector
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
the response. You can send [GET request](#get-a-settings-of-connectors) to see the state of connector.
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