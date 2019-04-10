# Ohara REST Interface

ohara provides a bunch of REST APIs to advanced user for managing data, applications and cluster.
Both request and response must have application/json content type, hence you should set content type to application/json in your request.
    
    Content-Type: application/json
    
and add content type of the response via the HTTP Accept header:

    Accept: application/json
    
----------
## Statuses & Errors

ohara leverages akka-http to support standards-compliant HTTP statuses. your clients should check the
HTTP status before parsing response entities. The error message in response body are format to json content.

```json
{
  "code": "java.lang.IllegalArgumentException",
  "message": "Unsupported restful api:vasdasd. Or the request is invalid to the vasdasd",
  "stack": "java.lang.IllegalArgumentException: Unsupported restful api:vasdasd. Or the request is invalid to the vasdasd at
}
```
    
1. code (**string**) — the type of error. It is normally a type of java exception
1. message (**string**) — a brief description of error
1. stack (**string**) — error stack captured by server
----------
## Topic

Ohara topic is based on kafka topic. It means the creation of topic on ohara will invoke a creation of kafka also.
Also, the delete to ohara topic also invoke a delete request to kafka. The common properties in topic are shown below.

1. id (**string**) — topic id
1. name (**string**) — topic name
1. brokerClusterName (**string**) — the broker cluster hosting this topic
1. numberOfReplications (**int**) — the number of replications for this topic
1. numberOfPartitions (**int**) — the number of partitions for this topic
1. lastModified (**long**) — the last time to update this topic

> Most properties are mapped to kafka. A little magic, but, is that ohara uses "id" rather than "name" in naming kafka topic.
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
1. numberOfReplications (optional **int**) (default: 1) — the number of replications for this topic
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