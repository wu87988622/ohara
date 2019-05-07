# User Guide

This documentation is for ohara users who try to exercise or test ohara without writing any code. Ohara team design and
implement ohara to provide a unparalleled platform which enable everyone to build streaming easily and quickly. For
normal users, you can manipulate ohara through UI interface even if you have no idea about the magic infra of ohara.
For advanced users trying to build custom streaming, they are encouraged to design and write application based on ohara's
various and powerful APIs (see [Custom Connector](custom_connector.md) and [Custom StreamApp](custom_streamapp.md)).

Ohara consists of many services, such as
1. [Configurator](#configurator) — the controller of ohara. It cooperates other services and provides the [Restful APIs](rest_interface.md) 
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

The core component of ohara is [Configurator](#configurator). After installing [related tools](#installation), you can
set up a Configurator via following docker command.

```bash
docker run --rm -p 12345:12345 oharastream/configurator:0.5-SNAPSHOT --port 12345
```

> click [here](#execute-configurator) to see more options for configurator

And then you can also create a manager to provide a beautiful UI based on above configurator.

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
- add-host: add a host mapping to /etc/hosts in configurator (nodeHostName:nodeHostIP)
- hostname: hostname to run configurator (defaults to 0.0.0.0)
- node: run a configurator with **pre-created** broker and zookeeper clusters (for testing purpose)

If `node` is not specified, the configurator will be running without **pre-created** zookeeper and broker clusters. You will need to create them manually
through configruator's RESTful APIs.

**NOTED:** you should prepare the broker and zookeeper images in each node where pre-created clusters will be running at if you want to use the `node` option

**NOTED:** you can enable the jmx reporter via inputing two env variables - "JMX_HOSTNAME" and "JMX_PORT".
- "JMX_HOSTNAME" should be same as the host running configurator container so as to access the jmx service in docker from outside.
- "JMX_PORT" should be opened by docker (for example, add "-p $JMX_PORT:JMX_PORT")

----------

### Execute Manager

```sh
docker run --rm -p 5050:5050 oharastream/manager:0.5-SNAPSHOT --port 5050 --configurator http://localhost:12345/v0
```
- port: bound by manager (default is 5050)
- configurator: basic form of restful API of configurator

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

## Configurator

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
