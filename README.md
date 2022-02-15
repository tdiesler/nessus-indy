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

## Start local nodes pool with docker

Here is how you can run a local nodes pool.

```
INDY_VERSION=1.16.0
INDY_CONTENT_URL="https://raw.githubusercontent.com/hyperledger/indy-sdk"
docker build -t nessusio/indy-pool "${INDY_CONTENT_URL}/v${INDY_VERSION}/ci/indy-pool.dockerfile"

docker run --detach \
  --name=indy-pool \
  -p 9701-9708:9701-9708 \
  nessusio/indy-pool
```

for the latest, use ...

```
INDY_CONTENT_URL="https://raw.githubusercontent.com/hyperledger/indy-sdk"
docker build -t nessusio/indy-pool "${INDY_CONTENT_URL}/master/ci/indy-pool.dockerfile"

docker run --detach \
  --name=indy-pool \
  -p 9701-9708:9701-9708 \
  nessusio/indy-pool
```

More details [here](https://github.com/hyperledger/indy-sdk#how-to-start-local-nodes-pool-with-docker)


## Getting Started with Indy SDK

[Getting Started](https://hyperledger-indy.readthedocs.io/projects/sdk/en/latest/docs/getting-started/)

```
docker-compose -f ./docs/getting-started/docker-compose.yml up
```

## Install libindy & indy-cli

How to install libindy is documented [here](https://github.com/hyperledger/indy-sdk/tree/master#installing-the-sdk)
and indy-cli [here](https://github.com/hyperledger/indy-sdk/tree/master/cli)

### CentOS

Install details are [here](https://github.com/hyperledger/indy-sdk#centos)

```
INDY_VERSION=1.16.0

# Build and install libindy dynamic lib
wget https://repo.sovrin.org/rpm/libindy/stable/${INDY_VERSION}/libindy.1.16.0.rpm
sudo yum install -y libsodium sqlite
sudo rpm -i libindy.${INDY_VERSION}.rpm

# Install indy-cli 
wget https://repo.sovrin.org/rpm/indy-cli/stable/${INDY_VERSION}/indy-cli.${INDY_VERSION}.rpm
sudo yum install -y ncurses-compat-libs openssl compat-openssl10 zeromq
sudo rpm -i indy-cli.${INDY_VERSION}.rpm
```

### MacOS

Install details are [here](https://github.com/hyperledger/indy-sdk#macos)

```
INDY_VERSION=1.16.0

# Build and install libindy dynamic lib
curl -s https://raw.githubusercontent.com/hyperledger/indy-sdk/v${INDY_VERSION}/libindy/mac.build.sh | sh

# Install indy-cli 
wget https://repo.sovrin.org/macos/indy-cli/stable/${INDY_VERSION}/indy-cli_${INDY_VERSION}.zip \
  && unzip -d indy-cli-${INDY_VERSION} indy-cli_${INDY_VERSION}.zip \
  && sudo mv indy-cli-${INDY_VERSION} /usr/local/opt/ \
  && sudo ln -s indy-cli-${INDY_VERSION} /usr/local/opt/indy-cli \
  && sudo ln -s ../opt/indy-cli/indy-cli /usr/local/bin/indy-cli
```
