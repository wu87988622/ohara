FROM ubuntu:16.04

# UPDATE
RUN apt-get -y update

# INSTALL apt-add-repository
RUN apt-get -q install --no-install-recommends -y software-properties-common

# INSTALL JDK
RUN add-apt-repository -y ppa:webupd8team/java
RUN apt-get -y update
RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
RUN apt-get install -y oracle-java8-installer
RUN mkdir /opt/java && ln -s /usr/lib/jvm/java-8-oracle /opt/java/default
ENV JAVA_HOME=/opt/java/default

# INSTALL apt-utils
RUN apt-get -q install --no-install-recommends -y apt-utils

# INSTALL tools
RUN apt-get -q install --no-install-recommends -y git
RUN apt-get -q install --no-install-recommends -y wget
RUN apt-get -q install --no-install-recommends -y vim
RUN apt-get -q install --no-install-recommends -y curl

# INSTALL Maven
RUN apt-get -q install --no-install-recommends -y maven

# INSTALL findbugs
RUN apt-get -q install --no-install-recommends -y findbugs
ENV FINDBUGS_HOME=/usr

# INSTALL python and pylint
RUN apt-get -q install --no-install-recommends -y python
RUN apt-get -q install --no-install-recommends -y python2.7
RUN apt-get -q install --no-install-recommends -y python-pip
RUN apt-get -q install --no-install-recommends -y python-pkg-resources
RUN apt-get -q install --no-install-recommends -y python-setuptools
RUN apt-get -q install --no-install-recommends -y python-wheel
RUN pip2 install pylint

# INSTALL ruby
RUN apt-get -q install --no-install-recommends -y ruby ruby-dev
RUN gem install rake
RUN gem install rubocop
RUN gem install ruby-lint

# INSTALL perl
RUN apt-get -q install --no-install-recommends -y perl libperl-critic-perl

# INSTALL shellcheck
RUN apt-get -q install --no-install-recommends -y shellcheck

# INSTALL net-tools
RUN apt-get -q install --no-install-recommends -y net-tools

# CLONE gradle
RUN mkdir /opt/gradle
RUN wget https://services.gradle.org/distributions/gradle-4.5-bin.zip
RUN unzip gradle-4.5-bin.zip -d /opt/gradle
RUN rm -f gradle-4.5-bin.zip
RUN ln -s /opt/gradle/gradle-4.5 /opt/gradle/default
ENV GRADLE_HOME=/opt/gradle/default

# UPDATE path
ENV PATH=$PATH:$GRADLE_HOME/bin
