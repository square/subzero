#!/usr/bin/env bash

set -euxo pipefail

cd "$(dirname $0)"/..
mkdir -p core/build
cd core/build
# For testnet. Generate compile_db for clang static analyzer
TARGET=dev CURRENCY=btc-testnet cmake ../ -DCMAKE_EXPORT_COMPILE_COMMANDS=ON
make
mv subzero subzero_testnet
# For mainnet. Skip compile_db generation
make clean
TARGET=dev CURRENCY=btc-mainnet cmake ../
make
mv subzero subzero_mainnet
