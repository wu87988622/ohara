# Ohara Stream

Easy to deploy the streaming application

[![Build Status](https://builds.is-land.com.tw/buildStatus/icon?job=PostCommit-OHARA)](https://builds.is-land.com.tw/job/PostCommit-OHARA/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![jFrog Bintray](https://img.shields.io/bintray/v/oharastream/ohara/ohara-client.svg)](https://bintray.com/oharastream/ohara)

----------

## Prerequisites

- OpenJDK 1.8
- Scala 2.12.8
- Gradle 5.1+
- Node.js 8.12.0
- Yarn 1.13.0 or greater
- Docker 18.09 or greater (Official QA is on docker 18.09. Also, docker multi-stage, which is supported by Docker 17.05 or higher, is required in building ohara images. see https://docs.docker.com/develop/develop-images/multistage-build/ for more details)

----------

## Gradle Commands

Ohara build is based on [gradle](https://gradle.org/). Ohara has defined many gradle tasks to simplify the development
of ohara.

----------

### Build Binary
```sh
gradle clean build -x test
```

> the tar file is located at ohara-assembly/distributions

----------

### Run All UTs
```sh
gradle clean test
```

> Ohara IT tests requires specific envs, and all IT tests will be skipped if you don't pass the related setting to IT.
  Ohara recommends you testing your code on [official QA](https://builds.is-land.com.tw/job/PreCommit-OHARA/) which
  offers the powerful machine and IT envs. 

----------

### Code Style Auto-Apply

Use this task to make sure your added code will have the same format and conventions with the rest of codebase.

```sh
gradle spotlessApply
```

> Note that we have this style check in early QA build.

----------

### License Auto-Apply

If you have added any new files in a PR. This task will automatically insert an Apache 2.0 license header in each one of these newly created files

```sh
gradle licenseApply
```

> Note that a file without the license header will fail at early QA build

----------

### Build Uber Jar

```sh
gradle clean uberJar -PskipManager
```

> the uber jar is under ohara-assembly/build/libs/

----------

### Publish Artifacts to JFrog Bintray

```sh
gradle clean build -PskipManager -x test bintrayUpload -PbintrayUser=$user -PbintrayKey=$key -PdryRun=false -Poverride=true
```
- bintrayUser: the account that has write permission to the repository
- bintrayKey: the account API Key
- dryRun: whether to publish artifacts (default is true)
- override: whether to override version artifacts already published

> Only release manager has permission to upload artifacts

----------

## Installation

We all love docker, right? All ohara services are executed by docker container. However, it is ok to run ohara services
through [assembly file](#build-binary) if you really really really hate docker.

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

## Ohara Team

- **Vito Jeng (vito@is-land.com.tw)** - leader
- **Jack Yang (jack@is-land.com.tw)** - committer
- **Chia-Ping Tsai (chia7712@is-land.com.tw)** - committer
- **Joshua_Lin (joshua@is-land.com.tw)** - committer
- **Geordie Mai (geordie@is-land.com.tw)** - committer
- **Yu-Chen Cheng (yuchen@is-land.com.tw)** - committer
- **Sam Cho (sam@is-land.com.tw)** - committer
- **Chih-Chiang Yeh (harryyeh@is-land.com.tw)** - committer
- **Harry Chiang (harry@is-land.com.tw)** - committer
- **Robert Ye (robertye@is-land.com.tw)** - committer

----------

## License

Ohara is an open source project and available under the Apache 2.0 License.
