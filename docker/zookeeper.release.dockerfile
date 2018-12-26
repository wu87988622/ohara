FROM centos:7.5.1804 as base

# install tools
RUN yum install -y \
  wget

# download zookeeper
ARG VERSION=3.4.13
RUN wget http://ftp.twaren.net/Unix/Web/apache/zookeeper/zookeeper-${VERSION}/zookeeper-${VERSION}.tar.gz
RUN tar -zxvf zookeeper-${VERSION}.tar.gz
RUN rm -f zookeeper-${VERSION}.tar.gz
RUN mkdir /opt/zookeeper
RUN mv zookeeper-${VERSION} /opt/zookeeper/
RUN echo "$VERSION" > $(find "/opt/zookeeper/" -maxdepth 1 -type d -name "zookeeper-*")/bin/true_version

# download Tini
ARG TINI_VERSION=v0.18.0
RUN wget https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini -O /tini

FROM centos:7.5.1804

# install tools
RUN yum install -y \
  java-1.8.0-openjdk

ENV JAVA_HOME=/usr/lib/jvm/jre

# change user
ARG USER=zookeeper
RUN groupadd $USER
RUN useradd -ms /bin/bash -g $USER $USER

# copy zookeeper binary
COPY --from=base /opt/zookeeper /home/$USER
RUN ln -s $(find "/home/$USER" -maxdepth 1 -type d -name "zookeeper-*") /home/$USER/default
COPY ./zk.sh /home/$USER/default/bin/
RUN chown -R $USER:$USER /home/$USER
RUN chmod +x /home/$USER/default/bin/zk.sh
ENV ZOOKEEPER_HOME=/home/$USER/default
ENV PATH=$PATH:$ZOOKEEPER_HOME/bin

# copy Tini
COPY --from=base /tini /tini
RUN chmod +x /tini

USER $USER

ENTRYPOINT ["/tini", "--", "zk.sh"]
