FROM centos:7.5.1804 as base

# install tools
RUN yum install -y \
  git \
  wget \
  unzip \
  java-1.8.0-openjdk-devel

ENV JAVA_HOME=/usr/lib/jvm/java

# download gradle
ARG GRADLE_VERSION=4.10.2
WORKDIR /opt/gradle
RUN wget --no-check-certificate https://downloads.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip
RUN unzip gradle-$GRADLE_VERSION-bin.zip
RUN rm -f gradle-$GRADLE_VERSION-bin.zip
ENV GRADLE_HOME=/opt/gradle/gradle-${GRADLE_VERSION}
ENV PATH=$PATH:$GRADLE_HOME/bin

# clone kafka
ARG GIT_REPO=https://github.com/apache/kafka.git
ARG BRANCH=trunk
WORKDIR /testpatch/kafka
RUN git clone --single-branch -b $BRANCH $GIT_REPO /testpatch/kafka
ARG SCALA_VERSION=2.11
RUN gradle clean install releaseTarGz -x signArchives -PscalaVersion=$SCALA_VERSION
RUN mkdir /opt/kafka
RUN tar -zxf $(find "/testpatch/kafka/core/build/distributions/" -maxdepth 1 -type f -name "*.tgz" -not -path "*site*") -C /opt/kafka/

# download Tini
# we download the Tini in multi-stage so as to save the space to install the wget
ARG TINI_VERSION=v0.18.0
RUN wget https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini -O /tini

FROM centos:7.5.1804

# install openjdk-1.8
RUN yum install -y \
  java-1.8.0-openjdk

ENV JAVA_HOME=/usr/lib/jvm/jre

# change user from root to kafka
ARG USER=kafka
RUN groupadd $USER
RUN useradd -ms /bin/bash -g $USER $USER

# copy kafka binary
WORKDIR /home/$USER
COPY --from=base /opt/kafka /home/$USER
RUN ln -s $(find "/home/$USER" -maxdepth 1 -type d -name "kafka_*") /home/$USER/default
ADD ./broker.sh /home/$USER/default/bin/
RUN chmod +x /home/$USER/default/bin/broker.sh
ADD ./worker.sh /home/$USER/default/bin/
RUN chmod +x /home/$USER/default/bin/worker.sh
RUN chown -R $USER:$USER /home/$USER
WORKDIR /home/$USER/default/bin/

# copy Tini
COPY --from=base /tini /tini
RUN chmod +x /tini

# change to user
USER $USER
WORKDIR /home/$USER

# Set ENV
ENV KAFKA_HOME=/home/$USER/default
ENV PATH=$PATH:$KAFKA_HOME/bin

ENTRYPOINT ["/tini", "--"]

CMD ["sh", "-c", "echo [Usage] broker.sh or worker.sh"]