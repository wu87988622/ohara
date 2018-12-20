FROM centos:7.5.1804 as base

# install tools
RUN yum install -y \
  wget

# download kafka
ARG VERSION=1.0.2
ARG SCALA_VERSION=2.11
RUN wget http://ftp.twaren.net/Unix/Web/apache/kafka/${VERSION}/kafka_${SCALA_VERSION}-${VERSION}.tgz
RUN tar -zxvf kafka_${SCALA_VERSION}-${VERSION}.tgz
RUN rm -f kafka_${SCALA_VERSION}-${VERSION}.tgz
RUN mkdir /opt/kafka
RUN mv kafka_${SCALA_VERSION}-${VERSION} /opt/kafka/

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
ARG USER=worker
RUN groupadd $USER
RUN useradd -ms /bin/bash -g $USER $USER

# copy kafka binary
# TODO: we should remove unused dependencies since this image is used to run broker only
COPY --from=base /opt/kafka /home/$USER
RUN ln -s $(find "/home/$USER" -maxdepth 1 -type d -name "kafka_*") /home/$USER/default
ADD ./worker.sh /home/$USER/default/bin/
RUN chmod +x /home/$USER/default/bin/worker.sh
RUN chown -R $USER:$USER /home/$USER
ENV KAFKA_HOME=/home/$USER/default
ENV PATH=$PATH:$KAFKA_HOME/bin

# copy Tini
COPY --from=base /tini /tini
RUN chmod +x /tini

# USER $USER

ENTRYPOINT ["/tini", "--", "worker.sh"]

