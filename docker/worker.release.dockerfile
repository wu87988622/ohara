FROM centos:7.6.1810 as deps

# install tools
RUN yum install -y \
  git \
  java-1.8.0-openjdk-devel \
  wget \
  unzip

# export JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java

# download kafka
ARG KAFKA_VERSION=1.0.2
ARG SCALA_VERSION=2.11
RUN wget https://archive.apache.org/dist/kafka/${KAFKA_VERSION}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz
RUN tar -zxvf kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz
RUN rm -f kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz
RUN mkdir /opt/kafka
RUN mv kafka_${SCALA_VERSION}-${KAFKA_VERSION} /opt/kafka/
RUN echo "$KAFKA_VERSION" > $(find "/opt/kafka/" -maxdepth 1 -type d -name "kafka_*")/bin/true_version

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
# TODO: we should clone ohara libs from official release... by chia
ARG OHARA_BRANCH="master"
WORKDIR /testpatch/ohara
RUN git clone --single-branch -b $OHARA_BRANCH https://github.com/oharastream/ohara.git /testpatch/ohara
# we build ohara with specified version of kafka in order to keep the compatibility
RUN gradle clean build -x test -PskipManager -Pkafka.version=$KAFKA_VERSION -Pscala.version=$SCALA_VERSION
RUN mkdir /opt/ohara
RUN tar -xvf $(find "/testpatch/ohara/ohara-assembly/build/distributions" -maxdepth 1 -type f -name "*.tar") -C /opt/ohara/
RUN cp $(find "/opt/ohara/" -maxdepth 1 -type d -name "ohara-*")/lib/* $(find "/opt/kafka/" -maxdepth 1 -type d -name "kafka_*")/libs/

# download Tini
# we download the Tini in multi-stage so as to save the space to install the wget
ARG TINI_VERSION=v0.18.0
RUN wget https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini -O /tini

FROM centos:7.6.1810

# install openjdk-1.8
# we use wget to download custom plugin from configurator
RUN yum install -y \
  java-1.8.0-openjdk \
  wget

ENV JAVA_HOME=/usr/lib/jvm/jre

# change user from root to kafka
ARG USER=worker
RUN groupadd $USER
RUN useradd -ms /bin/bash -g $USER $USER

# copy kafka binary
# TODO: we should remove unused dependencies since this image is used to run broker only
COPY --from=deps /opt/kafka /home/$USER
RUN ln -s $(find "/home/$USER" -maxdepth 1 -type d -name "kafka_*") /home/$USER/default
ADD ./worker.sh /home/$USER/default/bin/
RUN chmod +x /home/$USER/default/bin/worker.sh
RUN chown -R $USER:$USER /home/$USER
ENV KAFKA_HOME=/home/$USER/default
ENV PATH=$PATH:$KAFKA_HOME/bin

# copy Tini
COPY --from=deps /tini /tini
RUN chmod +x /tini

USER $USER

ENTRYPOINT ["/tini", "--", "worker.sh"]

