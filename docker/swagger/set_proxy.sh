#!/usr/bin/env bash
# set nginx proxy to configurator

# copy basic config
cp /etc/nginx/default.conf /etc/nginx/conf.d/default.conf
echo "proxy url :${CONFIGURATORURL}"
if [[ -z "${CONFIGURATORURL}" ]]; then
  sed -i -e 's@{proxyStting}@@g' /etc/nginx/conf.d/default.conf
else
  proxy="location /API/ { proxy_pass http://${CONFIGURATORURL}/;}"
  sed -i -e 's@{proxyStting}@'"${proxy}"'@g' /etc/nginx/conf.d/default.conf
fi
