# User Guide

This documentation is for ohara users who try to exercise or test ohara without writing any code. Ohara team design and
implement ohara to provide a unparalleled platform which enable everyone to build streaming easily and quickly. For
normal users, you can manipulate ohara through UI interface even if you have no idea about the magic infra of ohara.
For advanced users trying to build custom streaming, they are encouraged to design and write application based on ohara's
various and powerful APIs (see [Custom Connector](custom_connector.md) and [Custom StreamApp](custom_streamapp.md)).

----------

## Hello world

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
1. oharastream/broker:0.4-SNAPSHOT
1. oharastream/zookeeper:0.4-SNAPSHOT
1. oharastream/connect-worker:0.4-SNAPSHOT
1. oharastream/configurator:0.4-SNAPSHOT
1. oharastream/manager:0.4-SNAPSHOT
1. oharastream/streamapp:0.4-SNAPSHOT

----------

### Execute Ohara Configurator

```sh
docker run --rm -p ${port}:${port} --add-host ${nodeHostName}:${nodeHostIP} oharastream/configurator:0.4-SNAPSHOT --port ${port} --hostname ${host} --node ${SshUserName}:${SshPassword}@${NodeHostName}:${SshPort}
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

### Execute Ohara Manager

```sh
docker run --rm -p 5050:5050 oharastream/manager:0.4-SNAPSHOT --port 5050 --configurator http://localhost:12345/v0
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
docker run --rm -p 10000-10011:10000-10011 oharastream/backend:0.4-SNAPSHOT com.island.ohara.testing.service.FtpServer --controlPort 10000 --dataPorts 10001-10011 --user ${UserName} --password ${Password} --hostname ${hostIP or hostName}
```

- controlPort: bound by FTP Server
- dataPorts: bound by data transportation in FTP Server

----------