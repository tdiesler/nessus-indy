# Nessus Indy

With Nessus Indy we explore aspects of digital identy and verifiable credentials.

This is a contribution to "Making The World Work Better For All".

Who is going to control our digitial identity? Digital forms of our birth certificate, passport, drivers license,
medical records, vaccination certificates, univeristy degrees, property certificates, etc.
Is it the state, a corporation or should we be in control ourselves?

<img src="docs/img/ssi-book.png" height="200">

Shouldn't we have in fact a [self sovereign identity](https://www.manning.com/books/self-sovereign-identity) (SSI)?

## External Documentation

* [The Story of Open SSI Standards](https://www.youtube.com/watch?v=RllH91rcFdE)
* [Hyperledger Indy](https://hyperledger-indy.readthedocs.io)

## Getting Started with Indy SDK

```
docker-compose -f ~/git/nessus-indy/docker/getting-started.yml up
```

### Start local nodes pool with docker

Here is how you can run a local nodes pool.

```
docker run --detach \
  --name=indy-pool \
  -p 9701-9708:9701-9708 \
  nessusio/indy-pool

docker logs -f indy-pool
```

More details [here](https://github.com/hyperledger/indy-sdk#how-to-start-local-nodes-pool-with-docker)

### Install the indy-cli

How to install the indy-cli is documented [here](https://github.com/hyperledger/indy-sdk/tree/master/cli)

```
$ indy-cli

pool set-protocol-version 2
```
