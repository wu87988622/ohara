#
# Copyright 2019 is-land
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM openjdk:8u171-jdk-alpine as deps
MAINTAINER sam cho <sam@is-land.com.tw>

ARG KAFKA_VERSION=2.2.1
ARG LOG_VERSION=1.7.25
ARG GRADLE_VERSION=5.4.1
ARG COMMON_LANG_VERSION=3.7
ARG ROCKDB_VERSION=5.7.3
ARG COMMONS_IO=2.4

# download all dependency jars
RUN apk --no-cache add git curl && rm -rf /tmp/* /var/cache/apk/* && \
 mkdir -p /opt/lib/streamapp && \
 curl -L http://central.maven.org/maven2/org/apache/kafka/kafka-streams/${KAFKA_VERSION}/kafka-streams-${KAFKA_VERSION}.jar -o /opt/lib/kafka-streams.jar && \
 curl -L http://central.maven.org/maven2/org/apache/kafka/kafka-clients/${KAFKA_VERSION}/kafka-clients-${KAFKA_VERSION}.jar -o /opt/lib/kafka-clients.jar && \
 curl -L http://central.maven.org/maven2/org/slf4j/slf4j-api/${LOG_VERSION}/slf4j-api-${LOG_VERSION}.jar -o /opt/lib/slf4j-api.jar && \
 curl -L http://central.maven.org/maven2/org/slf4j/slf4j-simple/${LOG_VERSION}/slf4j-simple-${LOG_VERSION}.jar -o /opt/lib/slf4j-simple.jar && \
 curl -L http://central.maven.org/maven2/org/apache/commons/commons-lang3/${COMMON_LANG_VERSION}/commons-lang3-${COMMON_LANG_VERSION}.jar -o /opt/lib/commons-lang3.jar && \
 curl -L http://central.maven.org/maven2/org/rocksdb/rocksdbjni/${ROCKDB_VERSION}/rocksdbjni-${ROCKDB_VERSION}.jar -o /opt/lib/rocksdbjni.jar && \
 curl -L http://central.maven.org/maven2/commons-io/commons-io/${COMMONS_IO}/commons-io-${COMMONS_IO}.jar -o /opt/lib/commons-io.jar && \
 rm -rf /var/lib/apt/lists/*

# download gradle
WORKDIR /opt/gradle
RUN wget https://downloads.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip && \
 unzip gradle-$GRADLE_VERSION-bin.zip && \
 rm -f gradle-$GRADLE_VERSION-bin.zip && \
 ln -s /opt/gradle/gradle-$GRADLE_VERSION /opt/gradle/default

# add gradle to path
ENV GRADLE_HOME=/opt/gradle/default
ENV PATH=$PATH:$GRADLE_HOME/bin

# build ohara-streams
ARG BRANCH="master"
ARG COMMIT=$BRANCH
ARG REBASE=""
ARG REPO="https://github.com/oharastream/ohara.git"
WORKDIR /testpatch/ohara
RUN git clone $REPO /testpatch/ohara
RUN git checkout $COMMIT
RUN if [[ "$REBASE" != "" ]]; then git rebase $REBASE ; fi

# copy required jars except test jar
RUN gradle jar -x test && \
 cp `ls /testpatch/ohara/*/build/libs/*.jar | grep -v tests.jar | grep -E 'common|kafka|metrics|streams'` /opt/lib

FROM centos:7.6.1810

RUN yum -y update && \
 yum -y install java-1.8.0-openjdk-headless wget && \
 yum clean all && \
 rm -rf /var/cache/yum

# add user from root to kafka
ARG USER=ohara
RUN groupadd $USER
RUN useradd -ms /bin/bash -g $USER $USER

# copy required library
COPY --from=deps /opt/lib/* /opt/ohara/

# clone ohara binary
COPY --from=deps /testpatch/ohara/bin/* /home/$USER/default/
COPY --from=deps /testpatch/ohara/docker/streamapp.sh /home/$USER/default/
RUN mkdir /home/$USER/lib && \
 ln -s /opt/ohara/*.jar /home/$USER/lib && \
 chown -R $USER:$USER /home/$USER/default && \
 chmod +x /home/$USER/default/*.sh
ENV OHARA_HOME=/home/$USER/default
ENV PATH=$PATH:$OHARA_HOME

# add Tini
ARG TINI_VERSION=v0.18.0
RUN wget https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini -O /tini
RUN chmod +x /tini

# change user
USER $USER

ENTRYPOINT ["/tini", "--", "streamapp.sh"]