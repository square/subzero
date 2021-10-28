#pragma once

// maximum size for xpub. TODO: we can make this 112 bytes, based on:
// https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki#serialization-format
#define XPUB_SIZE 128

#ifdef BTC_MAINNET
  // This defines the gateway address used for transferring out of cold storage
  #define GATEWAY \
    "xpub68ititM5jRbzS14gpMD18Mo2VUqwwgfTu3EK2PjXTKeM69LSJvhrhGcbguicH313zwvZaoYtUwjU8vKoskUmsPJMjQ8oeZzVRmcBixXKELV"

    #define PUBKEY_PREFIX 0x0488b21e  // xpub
    #define PRIVKEY_PREFIX 0x0488ade4 // xprv
    #define BIP32_TEST_ROOT_PUBKEY "xpub661MyMwAqRbcGw6rpZ6SYUfFk6Z5YX216YRXnhuB6UcdwuVe4XUKKiPgbSoRXXtbYeRCghtGLtnJkG7r9d7WowaPHfDZQDqZCZwmyh2wHPf"
    #define BIP32_TEST_ROOT_PRIVKEY "xprv9s21ZrQH143K4T2PiXZSBLiXC4ib94J9jKVvzKVZY95f57AVWzA4mv5Ck9aEMRMcFAtMwrJuAjk69yQmUiL74uxhpGTK5QKo1yywVR2cd7q"
    #define BIP32_TEST_CHILD_PUBKEY "xpub6GaEayyNiA8rXNyFatszqAh8zr13rQHVHmJ5CQoqwp2S2xnmHjTNeLDE8tek12jjoSU25RivBv8E8SwFSMfaHecNummnzScZGd7TYX5evHa"
    #define BIP32_TEST_CHILD_PRIVKEY "xprvA3atBUSUsnaZJttnUsLzU2kQSpAZSwZdvYNUQ2QEPUVTAATckC986XtkHdrUBEiBFzB96yGBpn47VdE8n9ukPKffb5NuDMZejubExmLZuwr"

  #define COIN_TYPE 0

  // Generated with "turn inch relief grit abuse machine riot proof they model way dad pelican oven gold spoil cave
  // gloom dismiss dress leader scale isolate tribe"
  // m/0'
  #define TEST_WALLET_1 "xpub69MZ4d3eV3rRjQM5h35ohUaLYMNsj3VFCsou9ezUvLVc1KR82epLTwFEX5zTw2fpy2Z2UgfxuziKFSAtKd4xGCm3Lmo9kmWNcoUNAqB3Cpg"
  // m/0'/1
  #define TEST_WALLET_2 "xpub6BbKVUVkPk9GbPtERgd1wUeUQNRVQMW3pL7mneG3mDBYfw6aH7kFTaB8HuAhoPfoL5CkBNGwact362wTS1NGCdKCg9paNTaxaVhsyprQaJr"
  // m/0'/2
  #define TEST_WALLET_3  "xpub6BbKVUVkPk9GeQMvM9cobbyZZSgV84yjoqordLxUxAAVjuzhvFwJzxGA7Xwj7TuG2RY9g4eUKeREWZqZ9yxDSdLHeYBsrVFsic5gvix8RQF"
  // m/0'/3
  #define TEST_WALLET_4 "xpub6BbKVUVkPk9Ggyg4f5jeRjQK47c2UDbkTuD8G1YCwNwkckYATPQWSMZASAPsDtcsshKGTjob56KVXMRGqaeQ9Aenr8gk9ZGyHi76c2LbG7m"

  #define EXPECTED_SIGNATURE                                                 \
    {                                                                        \
     0x30, 0x44, 0x02, 0x20, 0x32, 0x22, 0xb4, 0x96, 0x91, 0x62, 0x99, 0x8c, \
     0x32, 0xb7, 0x71, 0x3a, 0x26, 0x57, 0x4b, 0x0b, 0x11, 0x12, 0x12, 0x01, \
     0x3b, 0xc9, 0x9b, 0xee, 0x09, 0x34, 0xc6, 0x5a, 0xda, 0x3f, 0xd5, 0x16, \
     0x02, 0x20, 0x40, 0x06, 0x3d, 0x96, 0xd1, 0x0a, 0xa9, 0x14, 0xc0, 0x66, \
     0x37, 0x9c, 0xba, 0xfe, 0xb9, 0x47, 0xf0, 0x1b, 0x79, 0xcc, 0x22, 0x35, \
     0x73, 0x9a, 0x8f, 0x3b, 0xb3, 0x42, 0x37, 0x84, 0x69, 0x2e}
  // echo 049dff0de94387941288cd672dd94b2c78046a42c60e4e0c7d7aa967eb32f7edef43021badba3ef585842b721aff9d7ee882c2bfc3a0345536b3a322694db0b803
  //  | xxd -r -p | xxd -i
  #define _QR_PUBKEY \
  { \
    0x04, 0x9d, 0xff, 0x0d, 0xe9, 0x43, 0x87, 0x94, 0x12, 0x88, 0xcd, 0x67, \
    0x2d, 0xd9, 0x4b, 0x2c, 0x78, 0x04, 0x6a, 0x42, 0xc6, 0x0e, 0x4e, 0x0c, \
    0x7d, 0x7a, 0xa9, 0x67, 0xeb, 0x32, 0xf7, 0xed, 0xef, 0x43, 0x02, 0x1b, \
    0xad, 0xba, 0x3e, 0xf5, 0x85, 0x84, 0x2b, 0x72, 0x1a, 0xff, 0x9d, 0x7e, \
    0xe8, 0x82, 0xc2, 0xbf, 0xc3, 0xa0, 0x34, 0x55, 0x36, 0xb3, 0xa3, 0x22, \
    0x69, 0x4d, 0xb0, 0xb8, 0x03 \
  }
#endif

#ifdef BTC_TESTNET
  // This defines the gateway address used for transferring out of cold storage
  // This is derived from BIP32 mnemonic below and derivation m/0'
  // cushion myth stove expose slice quarter wrestle fee bubble gown three pumpkin
  // WARNING: Obviously with the mnemonic above, this isn't secure.
  #define GATEWAY                                                                \
    "tpubDA3zCqbkh8BS1h3VGmB6VU1Cj638VJufBcDWSeZVw3aEE9rzvmrNoKyGFqDwr8d9rhf4sh" \
    "4Yjg8LVwkehVF3Aoyss1KdDFLoiarFJQvqp4R"
  #define PUBKEY_PREFIX 0x043587cf  // tpub
  #define PRIVKEY_PREFIX 0x04358394 // tprv
  #define BIP32_TEST_ROOT_PUBKEY "tpubD6NzVbkrYhZ4YjHiGk5XkPzd5DejXuX4eB1q9FxKSPNXhCAM8kKQUA4WqUtD42qqLYx7JHQ8szcMSScTYUxkhP2gMFxs5kVnnXYBjkRTZNr"
  #define BIP32_TEST_ROOT_PRIVKEY "tprv8ZgxMBicQKsPfGFvP6QwLzLWWC8oNaLA4sR3rjv227a8rhuaWMVpHfSefKjtMnjvccR8wwvfL6KtcpxWbvg3syEJLufcjm3qw5jMw5kQtRY"
  #define BIP32_TEST_CHILD_PUBKEY "tpubDHQd2YgckYt6DfiuccMCHnCHVDPLXnbdAAfZEr7nUG6uzeRvJj18sdut5TXjdeuFTL38zYrb8Ut6VXnrrQqgfKZVcZsQSUyZFM1yHATtcEZ"
  #define BIP32_TEST_CHILD_PRIVKEY "tprv8kiat8eNcBCRLCh7ixgbtNYAvBsQNTQias4mxL5V3zJXAAB9gLBYh9J1uL1QXURGBQW4CfiActGoaXeepUceyr3KNf9N8Who5hzsmyFabEE"

  #define COIN_TYPE 1

  #define TEST_WALLET_1 "tpubD9jBarsLCKot45kvTTu7yWxmNnkBPbpn2CgS1F3yuxZGgohTkamYwJJKenZHrsYwPRJY66dk3ZUt3aZwZqFf7QGsgUUUcNSvvb9NXHFt5Vb"
  #define TEST_WALLET_2 "tpubD8u5eHk8B6q63G4ekfBB2eVCeBYTrW2uCvi7Z3BF6bY2z33jW14Uzna8f5cFQWS3HFwdAzUWxqESq7j5x2CUQ7uBPgpXnpf8X9FPnh63XYd"
  #define TEST_WALLET_3 "tpubD9dkteiWZqa3jL17meqVvy1RSUQsmJvU3hBfAosMzrSa69DacRku8yHy3E2fma1Q4Den25ukcsBL3bYTFyKjKbF8CEWHu86Xg9YXiY6CkeC"
  #define TEST_WALLET_4 "tpubD8GrNWdYHDjdJryH5tng8LUzHSrEo4gToaguKnzthEqTxyfF13jTsp4sMtmso4n1VC58R5Wvt4Ua4npZTecR1xaGGYJgLLQj5sQGdD2xh2N"

  #define EXPECTED_SIGNATURE                                                   \
      {0x30, 0x44, 0x02, 0x20, 0x52, 0xcb, 0x0e, 0xe5, 0xdc, 0xd6, 0x45, 0x04, \
       0x04, 0x41, 0x25, 0xbe, 0x29, 0x43, 0xc1, 0x3a, 0x5d, 0xf8, 0xa7, 0x03, \
       0xc8, 0x28, 0xf0, 0xca, 0xb4, 0x84, 0x68, 0x1b, 0xb9, 0x11, 0xfa, 0x07, \
       0x02, 0x20, 0x67, 0xfc, 0x72, 0x67, 0x8e, 0xbf, 0xba, 0x94, 0x9b, 0x05, \
       0xf4, 0x7d, 0x96, 0xea, 0x91, 0x33, 0x96, 0x56, 0x4e, 0x44, 0xde, 0xbf, \
       0x17, 0x26, 0x8e, 0xbd, 0xdb, 0x51, 0x92, 0x70, 0xed, 0xfd}

  //leaving the dev private key here for debugging.
  //static const char * qr_private_key[] = {"3d9b97530af1d91e1d818c9498d8a53d9b97530af1d91e1d818c9498d8a59fe3"}; 
  // echo 049dff0de94387941288cd672dd94b2c78046a42c60e4e0c7d7aa967eb32f7edef43021badba3ef585842b721aff9d7ee882c2bfc3a0345536b3a322694db0b803
  //  | xxd -r -p | xxd -i
  #define _QR_PUBKEY \
  { \
    0x04, 0x9d, 0xff, 0x0d, 0xe9, 0x43, 0x87, 0x94, 0x12, 0x88, 0xcd, 0x67, \
    0x2d, 0xd9, 0x4b, 0x2c, 0x78, 0x04, 0x6a, 0x42, 0xc6, 0x0e, 0x4e, 0x0c, \
    0x7d, 0x7a, 0xa9, 0x67, 0xeb, 0x32, 0xf7, 0xed, 0xef, 0x43, 0x02, 0x1b, \
    0xad, 0xba, 0x3e, 0xf5, 0x85, 0x84, 0x2b, 0x72, 0x1a, 0xff, 0x9d, 0x7e, \
    0xe8, 0x82, 0xc2, 0xbf, 0xc3, 0xa0, 0x34, 0x55, 0x36, 0xb3, 0xa3, 0x22, \
    0x69, 0x4d, 0xb0, 0xb8, 0x03 \
  }

#endif

// Must match the values in java ui/shared.
#define MULTISIG_PARTS 4
#define MULTISIG_REQUIRED 2

#define PORT 32366

// VERSION_MAGIC is a constant picked randomly. Prevents accidentally swapping nvram between different applications.
#define VERSION_MAGIC 0x20de

// VERSION of this code. Must match the value in java/shared/src/main/java/com/squareup/subzero/shared/Constants.java
#define VERSION 210

// size of the VERSION file / nvram.
#define VERSION_SIZE 100

// max number of signers for SEE certs (nCipher specific)
#define SEE_CERT_SIGNER_SIZE 10

// amount of random bytes used for initializing wallets
#define MASTER_SEED_SIZE 64

#define COMPRESSED_PUBKEY_SIZE 33
