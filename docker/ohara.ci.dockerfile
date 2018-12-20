FROM centos:7.5.1804 AS deps

# install tools
RUN yum install -y \
  git \
  java-1.8.0-openjdk-devel \
  wget \
  unzip

# export JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java

# install dependencies for mysql
RUN yum install -y \
  libaio \
  numactl

# install nodejs
# NOTED: ohara-manager requires nodejs 8.x
RUN curl -sL https://rpm.nodesource.com/setup_8.x | bash -
RUN yum install -y nodejs

# install yarn
RUN npm install -g yarn@1.7.0

# install dependencies for cypress
RUN yum install -y \
  xorg-x11-server-Xvfb \
  gtk2-2.24* \
  libXtst* \
  libXScrnSaver* \
  GConf2* \
  alsa-lib*

# download gradle
ARG GRADLE_VERSION=4.10.3
WORKDIR /opt/gradle
RUN wget https://downloads.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip
RUN unzip gradle-$GRADLE_VERSION-bin.zip
RUN rm -f gradle-$GRADLE_VERSION-bin.zip

# add gradle to path
ENV GRADLE_HOME=/opt/gradle/gradle-$GRADLE_VERSION
ENV PATH=$PATH:$GRADLE_HOME/bin

# build ohara
ARG GIT_USER=""
ARG GIT_PWD=""
ARG BRANCH="master"
WORKDIR /testpatch/ohara
RUN git clone --single-branch -b $BRANCH https://$GIT_USER:$GIT_PWD@bitbucket.org/is-land/ohara.git /testpatch/ohara
# Running this test case make gradle download mysql binary code
RUN gradle clean build -x test -PskipManager
RUN gradle clean ohara-it:test --tests *TestDatabaseClient -PskipManager
# for cdh dependencies
RUN gradle -Pcdh clean build -x test

FROM centos:7.5.1804

ARG USER=jenkins

# install tools
RUN yum install -y \
  git \
  java-1.8.0-openjdk-devel \
  wget \
  unzip

# export JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java

# install dependencies for mysql
RUN yum install -y \
  libaio \
  numactl

# install nodejs
# NOTED: ohara-manager requires nodejs 8.x
RUN curl -sL https://rpm.nodesource.com/setup_8.x | bash -
RUN yum install -y nodejs

# install yarn
RUN npm install -g yarn@1.7.0

# install dependencies for cypress
RUN yum install -y \
  xorg-x11-server-Xvfb \
  gtk2-2.24* \
  libXtst* \
  libXScrnSaver* \
  GConf2* \
  alsa-lib*

# copy gradle
RUN mkdir -p /opt/gradle
COPY --from=deps /opt/gradle /opt/gradle
RUN ln -s $(find "/opt/gradle/" -maxdepth 1 -type d -name "gradle-*") /opt/gradle/default
ENV GRADLE_HOME=/opt/gradle/default
ENV PATH=$PATH:$GRADLE_HOME/bin

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

# see https://github.com/NixOS/nixpkgs/issues/20802
ENV GRADLE_USER_HOME=/home/$USER/.gradle
