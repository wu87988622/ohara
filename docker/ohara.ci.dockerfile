FROM ubuntu:18.04 AS deps

ARG USER=""
ARG PASSWORD=""

# update
RUN apt-get -y update

# copy repo
RUN apt-get -q install --no-install-recommends -y git
RUN apt-get -q install --no-install-recommends -y ca-certificates
WORKDIR /testpatch
RUN git clone https://$USER:$PASSWORD@bitbucket.org/is-land/ohara.git

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
RUN wget https://downloads.gradle.org/distributions/gradle-4.10-bin.zip
RUN unzip gradle-4.10-bin.zip
RUN rm -f gradle-4.10-bin.zip
RUN ln -s /opt/gradle/gradle-4.10 /opt/gradle/default
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
RUN mkdir -p /opt/gradle/gradle-4.10
COPY --from=deps /opt/gradle/gradle-4.10 /opt/gradle/gradle-4.10
RUN ln -s /opt/gradle/gradle-4.10 /opt/gradle/default
ENV GRADLE_HOME=/opt/gradle/default
ENV PATH=$PATH:$GRADLE_HOME/bin

# clone gradle dependencies
RUN mkdir /root/.gradle
COPY --from=deps /root/.gradle /root/.gradle

# clone yarn dependencies
RUN mkdir -p /root/.cache
COPY --from=deps /root/.cache /root/.cache

# clone database
RUN mkdir -p /root/.embedmysql
COPY --from=deps /root/.embedmysql /root/.embedmysql
