#!/usr/bin/env bash

set -euxo pipefail

cd "$(dirname $0)"/..
mkdir -p core/build
cd core/build
# For mainnet. Skip compile_db generation
TARGET=dev CURRENCY=btc-mainnet cmake ../ "$@"
make
if [ -f subzero ]; then
  mv subzero subzero-mainnet
fi
if [ -f subzero_fuzzer ]; then
  mv subzero_fuzzer subzero_fuzzer-mainnet
fi
# For testnet. Generate compile_db for clang static analyzer
make clean
TARGET=dev CURRENCY=btc-testnet cmake ../ -DCMAKE_EXPORT_COMPILE_COMMANDS=ON "$@"
make
if [ -f subzero ]; then
  mv subzero subzero-testnet
fi
if [ -f subzero_fuzzer ]; then
  mv subzero_fuzzer subzero_fuzzer-testnet
fi
