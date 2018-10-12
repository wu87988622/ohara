#!/usr/bin/env bash

# JVM options. Below is the default setting.
if [ -z "$OHARA_OPTS" ]; then
  export OHARA_OPTS="-Xmx4000m -XX:+UseConcMarkSweepGC"
fi
