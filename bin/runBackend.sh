#!/usr/bin/env bash

if [ -z "$SSH_PRIVATE_KEY" ]; then
  echo "Require ssh key to access ohara repository"
  exit 1
else
  # set up ssh key
  echo "-----BEGIN RSA PRIVATE KEY-----" > /root/.ssh/id_rsa
  echo "${SSH_PRIVATE_KEY}" >> /root/.ssh/id_rsa
  echo "-----END RSA PRIVATE KEY-----" >> /root/.ssh/id_rsa
  chmod 600 /root/.ssh/id_rsa
  mkdir /root/ohara
  git clone git@bitbucket.org:is-land/ohara.git /root/ohara
  cd /root/ohara
  if [ -z "$BRANCH" ]; then
    git checkout master
  else
    git checkout $BRANCH
  fi

  args=()
  if [ ! -z "$CONFIGURATOR_PORT" ]; then
    args+=("-Pport=$CONFIGURATOR_PORT")
  fi
  if [ ! -z "$TTL" ]; then
    args+=("-Pttl=$TTL")
  fi

  gradle runBackend $args
fi


