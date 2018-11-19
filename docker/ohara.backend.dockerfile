FROM centos:7.5.1804 AS deps

ARG BITBUCKET_USER=""
ARG BITBUCKET_PASSWORD=""
ARG GRADLE_VERSION=4.10.2
ARG BRANCH="master"

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
RUN git clone --single-branch -b $BRANCH https://$BITBUCKET_USER:$BITBUCKET_PASSWORD@bitbucket.org/is-land/ohara.git /testpatch/ohara
# Running this test case make gradle download mysql binary code
RUN gradle clean ohara-it:test --tests *TestDatabaseClient -PskipManager
RUN gradle clean build -x test -PskipManager
RUN mkdir /opt/ohara
RUN tar -xvf $(find "/testpatch/ohara/ohara-demo/build/distributions" -maxdepth 1 -type f -name "*.tar") -C /opt/ohara/

FROM centos:7.5.1804

ARG USER=ohara
ARG TINI_VERSION=v0.18.0

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
RUN wget https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini -O /tini
RUN chmod +x /tini

# change to user
USER $USER
WORKDIR /home/$USER

# Set ENV
ENV OHARA_HOME=/opt/ohara/default
ENV PATH=$PATH:$OHARA_HOME/bin

ENTRYPOINT ["/tini", "--", "/opt/ohara/default/bin/ohara", "start"]

CMD ["help"]