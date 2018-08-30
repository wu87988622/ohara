#!/usr/bin/env bash

if [ "$HOME" != "/root" ]; then
  echo "this script only servers for ohara image"
  exit 1
fi

if [ -d "/root/ohara_dist" ]; then
  echo "/root/ohara_dist exists!!!"
  exit 1
fi

if [ -z "$SSH_PRIVATE_KEY" ]; then
  echo "Require ssh key to access ohara repository"
  exit 1
else
  # set up ssh key
  echo "-----BEGIN RSA PRIVATE KEY-----" > /root/.ssh/id_rsa
  echo "${SSH_PRIVATE_KEY}" >> /root/.ssh/id_rsa
  echo "-----END RSA PRIVATE KEY-----" >> /root/.ssh/id_rsa
  chmod 600 /root/.ssh/id_rsa
  if [ ! -d "/root/ohara" ]; then
    mkdir /root/ohara
    git clone git@bitbucket.org:is-land/ohara.git /root/ohara
  fi
  cd /root/ohara
  git checkout -- . | git clean -df
  git pull
  if [ -z "$BRANCH" ]; then
    git checkout master
  else
    git checkout $BRANCH
  fi

  gradle distTar
  binarypath=$(find "/root/ohara/ohara-demo/build/distributions" -maxdepth 1 -type f -name "*.tar")
  mkdir /root/ohara_dist
  tar -xvf $binarypath -C /root/ohara_dist
  oharahome=$(find "/root/ohara_dist" -maxdepth 1 -type d -name "ohara-*")
  classpath="${oharahome}/lib/*:${oharahome}/*"
  # backend-service consist of 3-brokers, 3-workers, 1-mysql and 1-configurator
  heapSize="-Xmx4000m"
  if [ ! -z "$HEAP_SIZE" ]; then
    heapSize="-Xmx$HEAP_SIZE"
  fi
  java="java $heapSize -cp"
  class="com.island.ohara.demo.Backend"

  args=()
  if [ ! -z "$CONFIGURATOR_PORT" ]; then
    args+=("--port $CONFIGURATOR_PORT")
  fi
  if [ ! -z "$TTL" ]; then
    args+=("--ttl $TTL")
  fi

  $java $classpath $class $args > /root/backend.log 2>&1 &
  exec bash
fi