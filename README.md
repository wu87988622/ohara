# Ohara Stream

Easy to deploy the streaming application

[![Build Status](https://builds.is-land.com.tw/buildStatus/icon?job=PostCommit-OHARA)](https://builds.is-land.com.tw/job/PostCommit-OHARA/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![jFrog Bintray](https://img.shields.io/bintray/v/oharastream/ohara/ohara-client.svg)](https://bintray.com/oharastream/ohara)

----------

## Documentation

We use [Read the Docs](https://readthedocs.org/) for automating building and hosting.
Please goto following link to read document:

* [Latest](https://oharastream.readthedocs.io/en/latest/index.html) document
* Stable document(not available now)


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

see [User Guide](https://oharastream.readthedocs.io/en/latest/user_guide.html#installation)

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
