FROM centos:7.5.1804 AS deps

# install tools
RUN yum install -y \
  git \
  java-1.8.0-openjdk-devel \
  wget \
  unzip

# export JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java

# install nodejs
# NOTED: ohara-manager requires nodejs 8.x
RUN curl -sL https://rpm.nodesource.com/setup_8.x | bash -
RUN yum install -y nodejs

# install yarn
RUN npm install -g yarn@1.7.0

# download gradle
ARG GRADLE_VERSION=4.10.3
WORKDIR /opt/gradle
RUN wget https://downloads.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip
RUN unzip gradle-$GRADLE_VERSION-bin.zip
RUN rm -f gradle-$GRADLE_VERSION-bin.zip
RUN ln -s /opt/gradle/gradle-$GRADLE_VERSION /opt/gradle/default
ENV GRADLE_HOME=/opt/gradle/default
ENV PATH=$PATH:$GRADLE_HOME/bin

# build ohara
ARG GIT_USER=""
ARG GIT_PWD=""
ARG BRANCH="master"
WORKDIR /testpatch/ohara
RUN git clone --single-branch -b $BRANCH https://$GIT_USER:$GIT_PWD@bitbucket.org/is-land/ohara.git /testpatch/ohara
RUN gradle clean build -x test
RUN mkdir /opt/ohara
RUN tar -xvf $(find "/testpatch/ohara/ohara-assembly/build/distributions" -maxdepth 1 -type f -name "*.tar") -C /opt/ohara/

# remove configurator's dependencies
RUN rm -rf $(find "/opt/ohara/" -maxdepth 1 -type d -name "ohara-*")/lib

# Add Tini
ARG TINI_VERSION=v0.18.0
RUN wget https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini -O /tini

FROM centos:7.5.1804

# install nodejs
# NOTED: ohara-manager requires nodejs 8.x
RUN curl -sL https://rpm.nodesource.com/setup_8.x | bash -
RUN yum install -y nodejs

# install yarn
RUN npm install -g yarn@1.7.0

# add user
ARG USER=manager
RUN groupadd $USER
RUN useradd -ms /bin/bash -g $USER $USER

# clone ohara binary
COPY --from=deps /opt/ohara /home/$USER
RUN ln -s $(find "/home/$USER/" -maxdepth 1 -type d -name "ohara-*") /home/$USER/default
ENV OHARA_HOME=/home/$USER/default
ENV PATH=$PATH:$OHARA_HOME/bin

# clone Tini
COPY --from=deps /tini /tini
RUN chmod +x /tini

# change to user
USER $USER

ENTRYPOINT ["/tini", "--", "ohara", "start", "manager"]