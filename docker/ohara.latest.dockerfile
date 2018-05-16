FROM chia7712/ubuntu:18

# copy dependencies
RUN git clone https://bitbucket.org/is-land/ohara-dependencies.git
RUN cd ohara-dependencies && gradle resolveDependencies
RUN rm -rf ohara-dependencies
