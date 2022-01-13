#!/bin/bash

# How to start local nodes pool with docker
# https://github.com/hyperledger/indy-sdk#how-to-start-local-nodes-pool-with-docker

# IP to work with getting-started workbook
# pool_ip=10.0.0.2

pool_ip=127.0.0.1

if (( $# > 0 )); then
	pool_ip=$1
fi

if [[ -z $INDY_VERSION ]]; then
	INDY_VERSION=1.16.0
fi

docker build -t nessusio/indy-pool:${INDY_VERSION} \
  --build-arg pool_ip=${pool_ip} \
  -f ./docker/indy-pool.dockerfile ./docker

docker tag nessusio/indy-pool:${INDY_VERSION} nessusio/indy-pool
