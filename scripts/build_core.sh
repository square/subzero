#!/usr/bin/env bash

set -euxo pipefail

cd "$(dirname $0)"/..
mkdir -p core/build
cd core/build
# For mainnet. Skip compile_db generation
TARGET=dev CURRENCY=btc-mainnet cmake ../
make
mv subzero subzero-mainnet
# For testnet. Generate compile_db for clang static analyzer
make clean
TARGET=dev CURRENCY=btc-testnet cmake ../ -DCMAKE_EXPORT_COMPILE_COMMANDS=ON
make
mv subzero subzero-testnet
