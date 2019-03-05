## Why we need docker-compose

ohara is good at connecting to various systems to collect, transform, aggregate (and other operations you can imagine)
data. In order to test ohara, we need a way to run a bunch of systems simultaneously. We can build a heavy infra
to iron out this problem. Or we can leverage docker-compose to host various systems "locally" (yes, you need a powerful
machine to use ohara's docker-compose file).  

## Prerequisites

* Centos 7.6+ (supported by official community. However, other GNU/Linux should work well also)
* Docker 18.09+
* Docker-compose 1.23.2+

## How to install docker

This section is a clone of https://docs.docker.com/install/linux/docker-ce/centos/

### Uninstall old versions
```
$ sudo yum remove docker \
                  docker-client \
                  docker-client-latest \
                  docker-common \
                  docker-latest \
                  docker-latest-logrotate \
                  docker-logrotate \
                  docker-engine
```
### Install required packages
```
$ sudo yum install -y yum-utils \
  device-mapper-persistent-data \
  lvm2
```
### Install using the repository
```
$ sudo yum-config-manager \
    --add-repo \
    https://download.docker.com/linux/centos/docker-ce.repo
```
### Install docker-ce
```
$ sudo yum install docker-ce
```

## How to install Docker-compose
```
$ wget https://github.com/docker/compose/releases/download/1.23.2/docker-compose-Linux-x86_64 -O docker-compose
$ sudo chmod +x ./docker-compose
```
see https://github.com/docker/compose/releases for more details

## How to start services by docker-compose file
Before start services, you must set postgresql connection info for environment variable, example:
```
export POSTGRES_DB=postgres
export POSTGRES_USER=username
export POSTGRES_PASSWORD=password
```

Start services command
```
$ ./docker-compose -f {docker-compose file} up
```

## How to stop services
```
$ ctrl+c
```
We are talking about tests, right? We don't care about how to shutdown services gracefully

## How to cleanup all containers
```
$ docker rm -f $(docker ps -q -a)
```
We are talking about tests, right? You should have a machine for testing only so it is ok to remove all containers quickly.
That does simplify your work and life.

## How to enable IPv4 IP Forwarding

```
$ sudo vi /usr/lib/sysctl.d/00-system.conf
```
Add the following line.
```
net.ipv4.ip_forward=1
```
Save and exit the file. Restart network:
```
$ sudo systemctl restart network
```
see https://github.com/docker/compose/releases for more details