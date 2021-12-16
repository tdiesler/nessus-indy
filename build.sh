#!/bin/bash

INDY_VERSION=1.16.0

# How to start local nodes pool with docker
# https://github.com/hyperledger/indy-sdk#how-to-start-local-nodes-pool-with-docker

docker build -t nessusio/indy-pool:${INDY_VERSION} \
  "https://raw.githubusercontent.com/hyperledger/indy-sdk/v${INDY_VERSION}/ci/indy-pool.dockerfile"

docker tag nessusio/indy-pool:${INDY_VERSION} nessusio/indy-pool
