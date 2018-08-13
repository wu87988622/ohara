FROM chia7712/ubuntu:18 AS deps

# add credentials on build
ARG SSH_PRIVATE_KEY
RUN mkdir /root/.ssh/
RUN echo "-----BEGIN RSA PRIVATE KEY-----" > /root/.ssh/id_rsa
RUN echo "${SSH_PRIVATE_KEY}" >> /root/.ssh/id_rsa
RUN echo "-----END RSA PRIVATE KEY-----" >> /root/.ssh/id_rsa
RUN chmod 600 /root/.ssh/id_rsa

# make sure your domain is accepted
RUN ssh-keyscan bitbucket.org > /root/.ssh/known_hosts
RUN chmod 644 /root/.ssh/known_hosts

# copy repo
RUN git clone git@bitbucket.org:is-land/ohara.git
RUN cd ohara && gradle clean build
RUN ls /root/.gradle
RUN rm -rf /root/.ssh
RUN rm -rf ohara

FROM chia7712/ubuntu:18
# clone maven dependencies
RUN mkdir /root/.gradle
COPY --from=deps /root/.gradle /root/.gradle
# clone yarn dependencies
RUN mkdir -p /root/.cache
COPY --from=deps /root/.cache /root/.cache

