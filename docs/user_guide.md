# User Guide

This documentation is for ohara users who try to exercise or test ohara without writing any code. Ohara team design and
implement ohara to provide a unparalleled platform which enable everyone to build streaming easily and quickly. For
normal users, you can manipulate ohara through UI interface even if you have no idea about the magic infra of ohara.
For advanced users trying to build custom streaming, they are encouraged to design and write application based on ohara's
various and powerful APIs (see [Custom Connector](custom_connector.md) and [Custom StreamApp](custom_streamapp.md)).

Ohara consists of many services, such as
1. [Configurator](#ohara-configurator) — the controller of ohara. It cooperates other services and provides the [Restful APIs](rest_interface.md) 
1. [Manager](#manager) — the UI service of ohara. It offers a streaming flow called **pipeline** 
1. [Zookeeper](#zookeeper) — works for Broker. It has charge of managing the configuration of topics and health of node
1. [Broker](#broker) — It provides the access of topics, topics' data durability and balance.
1. [Worker](#worker) — It hosts and execute [Connectors](custom_connector.md) 
1. [Docker](#docker) — It packages the confis, dependencies and binary required by services and execute them in a isolated environments
1. and [Kubernetes](#kubernetes) — a management tool of docker instances

Ohara has a complicated software stack but most services are almost transparent to you. For example, before creating a
topic on ohara, you ought to set up a zookeeper cluster and a broker cluster. There are , unfortunately, a bunch of configs which
you have to design and write for both cluster. Ohara auto-generates most configs for you as best as it can, and ohara offers
the the readable [Restful APIs](rest_interface.md) and friendly UI to you. All complicated configs are replaced by some
simple steps showed on UI. The [Quick Start](#quick-start) section teach you to exercise ohara easily and quickly.

----------

## Quick Start

The core component of ohara is [Configurator](#ohara-configurator). After installing [related tools](#installation), you can
set up a Configurator via following docker command.

```bash
docker run --rm -p 12345:12345 oharastream/configurator:0.5-SNAPSHOT --port 12345
```

> click [here](#execute-configurator) to see more options for configurator

And then you can also create a manager to provide a beautiful UI based on above Ohara Configurator.

```bash
docker run --rm -p 5050:5050 oharastream/manager:0.5-SNAPSHOT --port 5050 --configurator http://$ip:12345/v0
```

> Please replace the **ip** by your host's address

Open your browser (we recommend [Google Chrome](https://www.google.com/intl/zh-TW/chrome/)) and link to http://localhost:5050.

----------

## Installation

We all love docker, right? All ohara services are executed by docker container. However, it is ok to run ohara services
through [assembly file](../README.md#build-binary) if you really really really hate docker.

----------

### Install Docker-ce on Centos

Docker has provided a great docs about installing docker-ce.
Please click this [link](https://docs.docker.com/install/linux/docker-ce/centos/).

----------

### Download Ohara Images

Ohara deploys docker images on [docker hub](https://hub.docker.com/u/oharastream). You can download images via `docker pull` command.
All images are list below.
1. oharastream/broker:0.5-SNAPSHOT
1. oharastream/zookeeper:0.5-SNAPSHOT
1. oharastream/connect-worker:0.5-SNAPSHOT
1. oharastream/configurator:0.5-SNAPSHOT
1. oharastream/manager:0.5-SNAPSHOT
1. oharastream/streamapp:0.5-SNAPSHOT

----------

### Execute Configurator

```sh
docker run --rm -p ${port}:${port} --add-host ${nodeHostName}:${nodeHostIP} oharastream/configurator:0.5-SNAPSHOT --port ${port} --hostname ${host} --node ${SshUserName}:${SshPassword}@${NodeHostName}:${SshPort}
```

- port: bound by Configurator (default is random)
- add-host: add a host mapping to /etc/hosts in Ohara Configurator (nodeHostName:nodeHostIP)
- hostname: hostname to run Ohara Configurator (defaults to 0.0.0.0)
- node: run a Ohara Configurator with **pre-created** broker and zookeeper clusters (for testing purpose)

If `node` is not specified, the Ohara Configurator will be running without **pre-created** zookeeper and broker clusters. You will need to create them manually
through configruator's RESTful APIs.

**NOTED:** you should prepare the broker and zookeeper images in each node where pre-created clusters will be running at if you want to use the `node` option

**NOTED:** you can enable the jmx reporter via inputing two env variables - "JMX_HOSTNAME" and "JMX_PORT".
- "JMX_HOSTNAME" should be same as the host running Ohara Configurator container so as to access the jmx service in docker from outside.
- "JMX_PORT" should be opened by docker (for example, add "-p $JMX_PORT:JMX_PORT")

----------

### Execute Manager

```sh
docker run --rm -p 5050:5050 oharastream/manager:0.5-SNAPSHOT --port 5050 --configurator http://localhost:12345/v0
```
- port: bound by manager (default is 5050)
- configurator: basic form of restful API of Ohara Configurator

----------

### Execute PostgreSQL Instance

```
docker run -d --rm --name postgresql -p 5432:5432 --env POSTGRES_DB=${DB_NAME} --env POSTGRES_USER=${USER_NAME} --env POSTGRES_PASSWORD=${PASSWORD} -it islandsystems/postgresql:9.2.24
```

- POSTGRES_DB: PostgreSQL DataBase name
- POSTGRES_USER: PostgreSQL login user name. **Note: POSTGRES_USER="user" is illegal to postgresql**
- POSTGRES_PASSWORD: PostgreSQL login password

----------

### Execute FTP Instance

```h
docker run --rm -p 10000-10011:10000-10011 oharastream/backend:0.5-SNAPSHOT com.island.ohara.testing.service.FtpServer --controlPort 10000 --dataPorts 10001-10011 --user ${UserName} --password ${Password} --hostname ${hostIP or hostName}
```

- controlPort: bound by FTP Server
- dataPorts: bound by data transportation in FTP Server

----------

## Ohara Configurator

Ohara consists of many services, and Ohara Configurator plays the most important rule which coordinates all services and
offers a bunch of restful APIs to user to get all under control. The brief architecture of Ohara Configurator is shown below.

![Configurator architecture](configurator_arch.jpg)

The introduction of each components are shown below. Feel free to trace the component in which you have interest.
- [Route of Ohara Configurator](#route-of-ohara-configurator)
- [Store of Ohara Configurator](#store-of-ohara-configurator)
- [Cache of Ohara Configurator](#cache-of-ohara-configurator)
- [Collie of Ohara Configurator](#collie-of-ohara-configurator)
- [Client of Ohara Configurator](#client-of-ohara-configurator)

----------

### Route of Ohara Configurator

Ohara Configurator leverages the akka-http to implements the rest server and handle the conversion of json objects. You
can click our [docs](rest_interface.md) to see all public APIs and introduction. 

The APIs supported by Ohara Configurator is only the Restful APIs. Of course, you can raise a question to us - why we 
choose the Restful APIs rather than pure Java APIs? The answer is - We all hate the other programming language except for
the one we are using. However, we always need to work with other people who are typing terrible and weired code,
and all they want to do is to call your APIs. In order to save our time from co-working with them, providing the
Restful APIs is always to be our solution. For another reason, Ohara Configurator is not in charge of I/O flow. Coordinating
all services requires small bandwidth only. We don't need to care for the performance issue about Restful APIs.

> You can use our internal scala APIs to control Configurator. The library is called ohara-client and it covers all Restful APIs of Configurator.
    However, we don't guarantee any compatibility for ohara-client.

----------

### Store of Ohara Configurator

All settings you request to Ohara Configurator are saved in Store, such as connector settings, cluster information and
pipeline description. The default implementation of Store is in-memory store which is implemented by java concurrent
hash map. The index of data is id stored by hash code so most GET requests to Ohara Configurator is cheap and fast. A
todo task, unfortunately, is that the in-memory store is not permanent so your Ohara Configurator instance will lose all
data after restarting, and this issue is traced by [here](https://github.com/oharastream/ohara/issues/185).

----------

### Cache of Ohara Configurator

The cost of coordinating countless services is the big **latency**. For example, [Topic APIs](rest_interface.md#topic)
allows you to fetch metrics from different [broker clusters](rest_interface.md#broker). Ohara Configurator has to file
a bunch of connections to different clusters to retrieve all requisite information, and, of course, the **connections**
bring the large latency to the GET request. Hence, Ohara Configurator sets up a inner cache which stores the data from
remote clusters. It reduces the latency from seconds to milliseconds and allay your anger. In order to make all data
up-to-date as much as possible, the cache auto-refreshes timeout data in the background. It brings some extra cost of
building connections to remote clusters.

----------

### Collie of Ohara Configurator

Apart from the data flow, Ohara Configurator is also doable to manage clusters for you. For instance, you can
1. add [node](rest_interface.md#node) to Ohara Configurator
1. deploy a [zookeeper cluster](rest_interface.md#zookeeper) on the node
1. deploy a [broker cluster](rest_interface.md#broker) on the node as well
1. deploy a [worker cluster](rest_interface.md#worker) on the node
1. finally, you can run a connector to stream your data and all services you have created are hosted by Ohara Configurator

In order to host your services safely and quickly, Ohara Configurator leverages the Docker technique that all services are packaged to
a container and executed on the node(s) specified by you. As a good software stack, Ohara Configurator creates a container
manager, which is called **collie*, to wrap Restful APIs of [k8s](#kubernetes) and ssh command to Scala APIs.

----------

### Client of Ohara Configurator

As a good programmer, we all love to reuse the code. However, it is hard to trust all third-party libraries guarantee
the suitable compatibility policy. The Client code in ohara is a collection of wrap for all client codes to services, such as
broker and worker, so as not to be badly hurt by the update of services.

----------

## Manager

----------

## Zookeeper

----------

## Broker

----------

## Worker

----------

## Docker

----------

## Kubernetes
