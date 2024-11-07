# Subzero

Codesafe code to perform Bitcoin wallet operations.

The code is designed to work on Mac OS X (for development purpose), as well as
nCipher. Requires glibc.

# Electrum
Electrum is a popular, open source wallet. The code is in python and it's sometimes useful
to compare bytes with Electrum.

    git clone git://github.com/spesmilo/electrum.git
    cd electrum
    cd contrib/build-osx

    # only need to build once
    ./make_osx
    cd ../../

    # you can then edit the code in lib/ and see the changes
    ./run_electrum --testnet -v

    # see also https://github.com/spesmilo/electrum/tree/master/contrib/build-osx

# Bitcoin resources
- [Mastering Bitcoin: Programming the Open Blockchain](https://www.amazon.com/Mastering-Bitcoin-Programming-Open-Blockchain/dp/1491954388/) is worth every penny. The book's latex source is free and you can build it yourself if you want a pdf version.
- Testnet: https://testnet.coinfaucet.eu/en/ and https://testnet.manu.backend.hamburg/faucet are two reliable faucets. https://live.blockcypher.com/btc-testnet/ is a testnet explorer.
- `brew install bx` is a very versatile tool.
- Bitcoin protocols are documented as [Bitcoin Improvement Proposals](https://github.com/bitcoin/bips), or BIP for short.
- Bip39 tools: https://iancoleman.io/bip39/, https://bip32jp.github.io/english/index.html. Keep in mind that some tools don't properly handle additional or missing `/` in the derivation path!
- Useful existing projects:
  - [Bitcoin Core](https://github.com/bitcoin/bitcoin/): the official full node (C++).
  - [BitcoinJ](https://github.com/bitcoinj/bitcoinj): used by BitBank (Java clone)
  - [Electrum](https://github.com/spesmilo/electrum): popular wallet software
  - Trezor: Hardware wallet.  [crypto](https://github.com/trezor/trezor-crypto) [firmware](https://github.com/trezor/trezor-mcu/tree/master/firmware), [common](https://github.com/trezor/trezor-common), etc.
- Useful StackExchange threads:
  - [How to redeem a basic Tx?](https://bitcoin.stackexchange.com/questions/3374/how-to-redeem-a-basic-tx)
  - [How to sign a transaction with multiple inputs?](https://bitcoin.stackexchange.com/questions/41209/how-to-sign-a-transaction-with-multiple-inputs/41226#41226)

# Getting started (docker)
This will build and start subzero in docker.

    git clone --recursive https://github.com/square/subzero.git
    ./subzero/core/subzero-docker.sh run

# Developing in CLion
Create a new project from this repo in CLion.
The IDE won't work properly until you hit "build", which generates proto header
files.  After you build in CLion, you may need to `File > Reload CMake Project`
for it to load the proto headers.

# Developing in Docker
See options with

    ./subzero-docker.sh help

If you want to iteratively develop in the docker container, you might want to:

    ./subzero-docker.sh dev

You now have a shell inside the container.
To rebuild:

    cd /build
    ./subzero --checks-only

You can continue to edit files outside the container, since it's a shared
filesystem with the host.

# Getting started (locally)

    git clone --recursive https://github.com/square/subzero.git
    cd subzero/core
    mkdir build
    cd build
    CURRENCY=btc-testnet cmake ../
    make
    ./subzero

# nCipher (without signed code)

    git clone --recursive https://github.com/square/subzero.git
    cd subzero/core
    mkdir build
    cd build
    TARGET=nCipher CURRENCY=btc-testnet cmake ../
    make
    make run
