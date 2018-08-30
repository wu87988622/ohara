FROM chia7712/ubuntu:18 AS deps

# add credentials on build
ARG SSH_PRIVATE_KEY
ARG BRANCH=master
ARG GRADLE_COMMAND="gradle clean build"
RUN mkdir /root/.ssh/
RUN echo "-----BEGIN RSA PRIVATE KEY-----" > /root/.ssh/id_rsa
RUN echo "${SSH_PRIVATE_KEY}" >> /root/.ssh/id_rsa
RUN echo "-----END RSA PRIVATE KEY-----" >> /root/.ssh/id_rsa
RUN chmod 600 /root/.ssh/id_rsa

# make sure your domain is accepted
RUN ssh-keyscan bitbucket.org > /root/.ssh/known_hosts
RUN chmod 644 /root/.ssh/known_hosts

# prepare folder
RUN mkdir /root/.embedmysql
RUN mkdir /root/.gradle

# copy repo
RUN mkdir /root/ohara
RUN git clone git@bitbucket.org:is-land/ohara.git /root/ohara
RUN cd /root/ohara && git checkout $BRANCH
RUN cd /root/ohara && $GRADLE_COMMAND

# setup scripts
RUN cp -r /root/ohara/quickstart /root/

# cleanup
RUN rm -rf /root/.ssh/id_rsa
RUN rm -rf /root/ohara

FROM chia7712/ubuntu:18

# clone maven dependencies
RUN mkdir /root/.gradle
COPY --from=deps /root/.gradle /root/.gradle

# clone yarn dependencies
RUN mkdir -p /root/.cache
COPY --from=deps /root/.cache /root/.cache

# clone database
RUN mkdir -p /root/.embedmysql
COPY --from=deps /root/.embedmysql /root/.embedmysql

# clone bitbucket key
RUN mkdir /root/.ssh
COPY --from=deps /root/.ssh/known_hosts /root/.ssh/known_hosts

# clone scripts
RUN mkdir /root/quickstart
COPY --from=deps /root/quickstart /root/quickstart

# setup env variables
ENV PATH=$PATH:/root/quickstart