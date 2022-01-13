#!/bin/bash

# How to start local nodes pool with docker
# https://github.com/hyperledger/indy-sdk#how-to-start-local-nodes-pool-with-docker

docker run --detach \
	--name=indy-pool \
	--net=host \
	-p 127.0.0.1:9701-9708:9701-9708 \
	nessusio/indy-pool:${INDY_VERSION:-latest}

docker logs -n200f indy-pool