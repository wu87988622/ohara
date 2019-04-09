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

FROM centos:7.6.1810 as deps

# install tools
RUN yum install -y \
  wget \
  git

# download kafka
# WARN: Please don't change the value of KAFKA_DIR
ARG KAFKA_DIR=/opt/kafka
ARG KAFKA_VERSION=1.0.2
ARG SCALA_VERSION=2.12
RUN wget https://archive.apache.org/dist/kafka/${KAFKA_VERSION}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz
RUN mkdir ${KAFKA_DIR}
RUN tar -zxvf kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz -C ${KAFKA_DIR}
RUN rm -f kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz
RUN echo "$KAFKA_VERSION" > $(find "${KAFKA_DIR}" -maxdepth 1 -type d -name "kafka_*")/bin/true_version

# download Prometheus exporter
ARG EXPORTER_VERSION=0.3.1
RUN mkdir /prometheus
RUN wget https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${EXPORTER_VERSION}/jmx_prometheus_javaagent-${EXPORTER_VERSION}.jar -O /prometheus/jmx_prometheus_javaagent.jar
ADD ./prometheus/exporter.config.yml /prometheus/config.yml

# clone ohara
ARG BRANCH="master"
ARG COMMIT=$BRANCH
ARG REPO="https://github.com/oharastream/ohara.git"
WORKDIR /testpatch/ohara
RUN git clone $REPO /testpatch/ohara
RUN git checkout $COMMIT

FROM centos:7.6.1810

# install openjdk-1.8
RUN yum install -y \
  java-1.8.0-openjdk

ENV JAVA_HOME=/usr/lib/jvm/jre

# change user from root to kafka
ARG USER=broker
RUN groupadd $USER
RUN useradd -ms /bin/bash -g $USER $USER

# copy kafka binary
# TODO: we should remove unused dependencies since this image is used to run broker only
COPY --from=deps /opt/kafka /home/$USER
RUN ln -s $(find "/home/$USER" -maxdepth 1 -type d -name "kafka_*") /home/$USER/default
COPY --from=deps /testpatch/ohara/docker/broker.sh /home/$USER/default/bin/
RUN chmod +x /home/$USER/default/bin/broker.sh
RUN chown -R $USER:$USER /home/$USER
ENV KAFKA_HOME=/home/$USER/default
ENV PATH=$PATH:$KAFKA_HOME/bin

# copy prometheus java-exporter
COPY --from=deps /prometheus /prometheus
ENV PROMETHEUS_EXPORTER=/prometheus/jmx_prometheus_javaagent.jar
ENV PROMETHEUS_EXPORTER_CONFIG=/prometheus/config.yml

# copy Tini
COPY --from=oharastream/ohara:deps /tini /tini
RUN chmod +x /tini

USER $USER

ENTRYPOINT ["/tini", "--", "broker.sh"]
