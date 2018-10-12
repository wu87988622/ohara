FROM ubuntu:18.04 AS deps

ARG BITBUCKET_USER=""
ARG BITBUCKET_PASSWORD=""
ARG GRADLE_VERSION=4.10.2

# update
RUN apt-get -y update

# copy repo
RUN apt-get -q install --no-install-recommends -y git
RUN apt-get -q install --no-install-recommends -y ca-certificates
WORKDIR /testpatch
RUN git clone https://BITBUCKET_USER:BITBUCKET_PASSWORD@bitbucket.org/is-land/ohara.git

# install build tool
RUN apt-get -q install --no-install-recommends -y apt-utils
RUN apt-get -q install --no-install-recommends -y openjdk-8-jdk
RUN apt-get -q install --no-install-recommends -y wget
RUN apt-get -q install --no-install-recommends -y unzip
RUN apt-get -q install --no-install-recommends -y curl
RUN apt-get -q install --no-install-recommends -y gnupg
RUN apt-get -q install --no-install-recommends -y gnupg1
RUN apt-get -q install --no-install-recommends -y gnupg2
RUN apt-get -q install --no-install-recommends -y node.js

# native libraries of mysql
RUN apt-get -q install --no-install-recommends -y libaio1
RUN apt-get -q install --no-install-recommends -y libnuma1

# dependencies for cypress
RUN apt-get -q install --no-install-recommends -y xvfb
RUN apt-get -q install --no-install-recommends -y libgtk2.0-0
RUN apt-get -q install --no-install-recommends -y libnotify-dev
RUN apt-get -q install --no-install-recommends -y libgconf-2-4
RUN apt-get -q install --no-install-recommends -y libnss3
RUN apt-get -q install --no-install-recommends -y libxss1
RUN apt-get -q install --no-install-recommends -y libasound2

# INSTALL yarn
RUN apt install --no-install-recommends -y gpg-agent
RUN curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add -
RUN echo "deb https://dl.yarnpkg.com/debian/ stable main" | tee /etc/apt/sources.list.d/yarn.list
RUN apt-get -y update
RUN apt-get -q install --no-install-recommends -y yarn=1.7.0-1

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
RUN gradle clean ohara-configurator:test --tests *TestDatabaseClient -PskipManager
RUN gradle clean build -x test -PskipManager
# for cdh dependencies
RUN gradle -Pcdh clean build -x test

FROM ubuntu:18.04

ARG USER=jenkins
ARG GRADLE_VERSION=4.10.2

# update
RUN apt-get -y update
RUN apt-get -q install --no-install-recommends -y apt-utils
RUN apt-get -q install --no-install-recommends -y openjdk-8-jdk
RUN apt-get -q install --no-install-recommends -y git
RUN apt-get -q install --no-install-recommends -y curl
RUN apt-get -q install --no-install-recommends -y gnupg
RUN apt-get -q install --no-install-recommends -y gnupg1
RUN apt-get -q install --no-install-recommends -y gnupg2
RUN apt-get -q install --no-install-recommends -y node.js

# native libraries of mysql
RUN apt-get -q install --no-install-recommends -y libaio1
RUN apt-get -q install --no-install-recommends -y libnuma1

# dependencies for cypress
RUN apt-get -q install --no-install-recommends -y xvfb
RUN apt-get -q install --no-install-recommends -y libgtk2.0-0
RUN apt-get -q install --no-install-recommends -y libnotify-dev
RUN apt-get -q install --no-install-recommends -y libgconf-2-4
RUN apt-get -q install --no-install-recommends -y libnss3
RUN apt-get -q install --no-install-recommends -y libxss1
RUN apt-get -q install --no-install-recommends -y libasound2

# INSTALL yarn
RUN apt install --no-install-recommends -y gpg-agent
RUN curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add -
RUN echo "deb https://dl.yarnpkg.com/debian/ stable main" | tee /etc/apt/sources.list.d/yarn.list
RUN apt-get -y update
RUN apt-get -q install --no-install-recommends -y yarn=1.7.0-1

# copy gradle
RUN mkdir -p /opt/gradle/gradle-$GRADLE_VERSION
COPY --from=deps /opt/gradle/gradle-$GRADLE_VERSION /opt/gradle/gradle-$GRADLE_VERSION
RUN ln -s /opt/gradle/gradle-$GRADLE_VERSION /opt/gradle/default

# change user
RUN groupadd $USER
RUN useradd -ms /bin/bash -g $USER $USER

# copy gradle dependencies
RUN mkdir /home/$USER/.gradle
# TODO: use --chown if https://github.com/moby/moby/issues/35018 is fixed
COPY --from=deps /root/.gradle /home/$USER/.gradle
RUN chown -R $USER:$USER /home/$USER/.gradle

# clone yarn dependencies
RUN mkdir -p /home/$USER/.cache
COPY --from=deps /root/.cache /home/$USER/.cache
RUN chown -R $USER:$USER /home/$USER/.cache

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
