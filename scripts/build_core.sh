#!/usr/bin/env bash

set -euxo pipefail

cd "$(dirname $0)"/..
mkdir -p core/build
cd core/build
TARGET=dev CURRENCY=btc-testnet cmake ../
make
