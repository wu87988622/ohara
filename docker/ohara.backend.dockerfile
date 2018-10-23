FROM ubuntu:18.04 AS deps

ARG BITBUCKET_USER=""
ARG BITBUCKET_PASSWORD=""
ARG GRADLE_VERSION=4.10.2
ARG BRANCH="master"

# update
RUN apt-get -y update

# copy repo
RUN apt-get -q install --no-install-recommends -y git
RUN apt-get -q install --no-install-recommends -y ca-certificates
WORKDIR /testpatch
RUN git clone --single-branch -b $BRANCH https://$BITBUCKET_USER:$BITBUCKET_PASSWORD@bitbucket.org/is-land/ohara.git

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
RUN git checkout $BRANCH
# Running this test case make gradle download mysql binary code
RUN gradle clean ohara-it:test --tests *TestDatabaseClient -PskipManager
RUN gradle clean build -x test -PskipManager
RUN mkdir /opt/ohara
RUN tar -xvf $(find "/testpatch/ohara/ohara-demo/build/distributions" -maxdepth 1 -type f -name "*.tar") -C /opt/ohara/

FROM ubuntu:18.04

ARG USER=ohara
ARG TINI_VERSION=v0.18.0

# update
RUN apt-get -y update
RUN apt-get -q install --no-install-recommends -y apt-utils
RUN apt-get -q install --no-install-recommends -y openjdk-8-jdk

# native libraries of mysql
RUN apt-get -q install --no-install-recommends -y libaio1
RUN apt-get -q install --no-install-recommends -y libnuma1

# add user
RUN groupadd $USER
RUN useradd -ms /bin/bash -g $USER $USER

# clone ohara binary
RUN mkdir /opt/ohara
COPY --from=deps /opt/ohara /opt/ohara
RUN ln -s $(find "/opt/ohara/" -maxdepth 1 -type d -name "ohara-*") /opt/ohara/default
# (TODO) manager has got to write something to binary folder...we should keep the permission if OHARA-669 is resolved
RUN chown -R $USER:$USER /opt/ohara

# clone database
RUN mkdir -p /home/$USER/.embedmysql
COPY --from=deps /root/.embedmysql /home/$USER/.embedmysql
RUN chown -R $USER:$USER /home/$USER/.embedmysql

# Add Tini
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini /tini
RUN chmod +x /tini

# change to user
USER $USER
WORKDIR /home/$USER

# Set ENV
ENV OHARA_HOME=/opt/ohara/default
ENV PATH=$PATH:$OHARA_HOME/bin

ENTRYPOINT ["/tini", "--", "/opt/ohara/default/bin/ohara", "start"]

CMD ["help"]