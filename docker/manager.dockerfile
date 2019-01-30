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

FROM oharastream/ohara:deps as deps

ARG BRANCH="master"
ARG COMMIT=$BRANCH
ARG REPO="https://github.com/oharastream/ohara.git"
WORKDIR /testpatch/ohara
RUN git clone $REPO /testpatch/ohara
RUN git checkout $COMMIT
RUN gradle clean build -x test
RUN mkdir /opt/ohara
RUN tar -xvf $(find "/testpatch/ohara/ohara-assembly/build/distributions" -maxdepth 1 -type f -name "*.tar") -C /opt/ohara/

# Add Tini
ARG TINI_VERSION=v0.18.0
RUN wget https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini -O /tini

FROM centos:7.6.1810

# install nodejs
# NOTED: ohara-manager requires nodejs 8.x
RUN curl -sL https://rpm.nodesource.com/setup_8.x | bash -
RUN yum install -y nodejs

# install yarn
RUN npm install -g yarn@1.7.0

# add user
ARG USER=ohara
RUN groupadd $USER
RUN useradd -ms /bin/bash -g $USER $USER

# clone ohara binary
COPY --from=deps /opt/ohara /home/$USER
RUN ln -s $(find "/home/$USER/" -maxdepth 1 -type d -name "ohara-*") /home/$USER/default
ENV OHARA_HOME=/home/$USER/default
ENV PATH=$PATH:$OHARA_HOME/bin

# clone Tini
COPY --from=deps /tini /tini
RUN chmod +x /tini

# change to user
USER $USER

ENTRYPOINT ["/tini", "--", "ohara.sh", "start", "manager"]