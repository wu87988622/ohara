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

ARG KAFKA_VERSION=1.0.2
ARG LOG_VERSION=1.7.25
ARG GRADLE_VERSION=5.1.1

RUN apk --no-cache add git curl && rm -rf /tmp/* /var/cache/apk/* && \
 mkdir -p /opt/lib/streamapp && \
 curl -L http://central.maven.org/maven2/org/apache/kafka/kafka-streams/${KAFKA_VERSION}/kafka-streams-${KAFKA_VERSION}.jar -o /opt/lib/kafka-streams.jar && \
 curl -L http://central.maven.org/maven2/org/apache/kafka/kafka-clients/${KAFKA_VERSION}/kafka-clients-${KAFKA_VERSION}.jar -o /opt/lib/kafka-clients.jar && \
 curl -L http://central.maven.org/maven2/org/slf4j/slf4j-api/${LOG_VERSION}/slf4j-api-${LOG_VERSION}.jar -o /opt/lib/slf4j-api.jar && \
 curl -L http://central.maven.org/maven2/org/slf4j/slf4j-simple/${LOG_VERSION}/slf4j-simple-${LOG_VERSION}.jar -o /opt/lib/slf4j-simple.jar && \
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
ARG REPO="https://github.com/oharastream/ohara.git"
WORKDIR /testpatch/ohara
RUN git clone $REPO /testpatch/ohara
RUN git checkout $COMMIT
RUN gradle :ohara-streams:jar -x test && \
 cp /testpatch/ohara/ohara-streams/build/libs/*.jar /opt/lib

FROM openjdk:8u171-jre-alpine

# add tini
RUN apk add --no-cache tini

WORKDIR /opt/ohara

COPY --from=deps /opt/lib/* /opt/ohara/

VOLUME ["/opt/ohara/streamapp"]

ENTRYPOINT ["/sbin/tini", "--", "/usr/bin/java", "-cp", "/opt/ohara/*:/opt/ohara/streamapp/*"]