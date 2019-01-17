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

FROM centos:7.6.1810 as base

# install tools
RUN yum install -y \
  wget

ARG PROMETHEUS_HOME=/opt/prometheus

#download prometheus
ARG VERSION=2.6.0
RUN wget https://github.com/prometheus/prometheus/releases/download/v${VERSION}/prometheus-${VERSION}.linux-amd64.tar.gz
RUN tar -zxvf prometheus-${VERSION}.linux-amd64.tar.gz
RUN rm -f prometheus-${VERSION}.linux-amd64.tar.gz
RUN mkdir $PROMETHEUS_HOME
RUN mv prometheus-${VERSION}.linux-amd64/* $PROMETHEUS_HOME
RUN ls  /opt/prometheus

ARG PROMETHEUS_CONFIG=$PROMETHEUS_HOME/config
RUN mkdir $PROMETHEUS_CONFIG
ADD prometheus/prometheus.yml $PROMETHEUS_CONFIG
#basic empty target json
ADD prometheus/targets.json $PROMETHEUS_CONFIG
ADD prometheus/target.sh $PROMETHEUS_CONFIG
RUN mkdir $PROMETHEUS_CONFIG/targets

# download Tini
# we download the Tini in multi-stage so as to save the space to install the wget
ARG TINI_VERSION=v0.18.0
RUN wget https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini -O /tini

FROM centos:7.6.1810
# install jq (json framework)
RUN yum install epel-release -y
RUN yum install jq -y

# change user from root to prometheus
ARG USER=prometheus
RUN groupadd $USER
RUN useradd -ms /bin/bash -g $USER $USER

# copy prometheus binary
WORKDIR /home/$USER
COPY --from=base /opt/prometheus /home/$USER
RUN chmod +x /home/$USER/config/target.sh
RUN chown $USER /home/$USER/config/targets

# copy Tini
COPY --from=base /tini /tini
RUN chmod +x /tini

# change to user
USER $USER
WORKDIR /home/$USER

# Set ENV
ENV PROMETHEUS_HOME=/home/$USER

ENTRYPOINT ["/tini", "--" ,"./config/target.sh"]
