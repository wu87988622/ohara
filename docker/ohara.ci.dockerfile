FROM ubuntu:18.04 AS deps

ARG BITBUCKET_USER=""
ARG BITBUCKET_PASSWORD=""
ARG GRADLE_VERSION=4.10.2
ARG BRANCH="master"

# update
RUN apt-get -y update && apt-get -q install --no-install-recommends -y \
  git \
  ca-certificates \
  apt-utils \
  openjdk-8-jdk \
  wget \
  unzip \
  gnupg \
  gnupg1 \
  gnupg2 \
  node.js \
  libaio1 \
  libnuma1 \
  gpg-agent \
  npm \
  xvfb \
  libgtk2.0-0 \
  libnotify-dev \
  libgconf-2-4 \
  libnss3 \
  libxss1 \
  libasound2

# copy repo
WORKDIR /testpatch
RUN git clone --single-branch -b $BRANCH https://$BITBUCKET_USER:$BITBUCKET_PASSWORD@bitbucket.org/is-land/ohara.git

# INSTALL yarn
RUN npm install -g yarn@1.7.0

# download gradle
WORKDIR /opt/gradle
RUN wget https://downloads.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip
RUN unzip gradle-$GRADLE_VERSION-bin.zip
RUN rm -f gradle-$GRADLE_VERSION-bin.zip
RUN ln -s /opt/gradle/gradle-$GRADLE_VERSION /opt/gradle/default

# add gradle to path
ENV GRADLE_HOME=/opt/gradle/default
ENV PATH=$PATH:$GRADLE_HOME/bin

# build ohara
WORKDIR /testpatch/ohara
# Running this test case make gradle download mysql binary code
RUN gradle clean build -x test -PskipManager
RUN gradle clean ohara-it:test --tests *TestDatabaseClient -PskipManager
# for cdh dependencies
RUN gradle -Pcdh clean build -x test

FROM ubuntu:18.04

ARG USER=jenkins
ARG GRADLE_VERSION=4.10.2

# update
RUN apt-get -y update && apt-get -q install --no-install-recommends -y \
  git \
  ca-certificates \
  apt-utils \
  openjdk-8-jdk \
  gnupg \
  gnupg1 \
  gnupg2 \
  node.js \
  libaio1 \
  libnuma1 \
  gpg-agent \
  npm \
  xvfb \
  libgtk2.0-0 \
  libnotify-dev \
  libgconf-2-4 \
  libnss3 \
  libxss1 \
  libasound2

# INSTALL yarn
RUN npm install -g yarn@1.7.0

# copy gradle
RUN mkdir -p /opt/gradle/gradle-$GRADLE_VERSION
COPY --from=deps /opt/gradle/gradle-$GRADLE_VERSION /opt/gradle/gradle-$GRADLE_VERSION
RUN ln -s /opt/gradle/gradle-$GRADLE_VERSION /opt/gradle/default

# add user
RUN groupadd $USER
RUN useradd -ms /bin/bash -g $USER $USER

# copy gradle dependencies
RUN mkdir /home/$USER/.gradle
# TODO: use --chown if https://github.com/moby/moby/issues/35018 is fixed
COPY --from=deps /root/.gradle /home/$USER/.gradle
RUN chown -R $USER:$USER /home/$USER/.gradle

# clone database instance
RUN mkdir -p /home/$USER/.embedmysql
COPY --from=deps /root/.embedmysql /home/$USER/.embedmysql
RUN chown -R $USER:$USER /home/$USER/.embedmysql

# change to user
USER $USER
WORKDIR /home/$USER

# add gradle to path
ENV GRADLE_HOME=/opt/gradle/default
ENV PATH=$PATH:$GRADLE_HOME/bin

# see https://github.com/NixOS/nixpkgs/issues/20802
ENV GRADLE_USER_HOME=/home/$USER/.gradle
