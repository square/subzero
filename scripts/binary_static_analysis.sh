#!/usr/bin/env bash

# This is a simple static analysis tool for subzero core. It checks 
# - which gateway address is present in the subzero binary. Gateway address
# types are: "TESTNET", "MAINNET", "UNKNOWN"
# - which AES-GCM key type is present in the subzero binary. Key types are:
# "DEV", "NCIPHER", "UNKNOWN"

set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 /path/to/subzero_binary"
    exit 1
fi

#########################################
# SHA256 digests of gateway addresses
#########################################
# TESTNET gateway
# echo -n "tpubDA3zCqbkh8BS1h3VGmB6VU1Cj638VJufBcDWSeZVw3aEE9rzvmrNoKyGFqDwr8d9rhf4sh4Yjg8LVwkehVF3Aoyss1KdDFLoiarFJQvqp4R" | sha256sum
# Gateway address is from core/include/config.h
GATEWAY_TESTNET_SHA256SUM="f772a01ae01fd49179e3d8c18d7cc95374bb5055323ef9b08fc1dd8dabd6e05f"

# MAINNET gateway
# echo -n "xpub68ititM5jRbzS14gpMD18Mo2VUqwwgfTu3EK2PjXTKeM69LSJvhrhGcbguicH313zwvZaoYtUwjU8vKoskUmsPJMjQ8oeZzVRmcBixXKELV" | sha256sum
# Gatewy address is from core/include/config.h
GATEWAY_MAINNET_SHA256SUM="2441ac96a8102ae06412f3c7082077f29332efc17e121f314442c3e279ea6d07"

# SHA256 digest of empty string, for debugging reference
# EMPYT_STRING_SHA256SUM="e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

#########################################
# AES key type magic strings
#########################################
# Shall be in sync with MAGIC value in core/include/aes_gcm_dev.h
MAGIC_DEV="aes_gcm_dev:8873b8689d31cc4d"
# Shall be in sync with MAGIC value in core/include/aes_gcm_ncipher.h
MAGIC_NCIPHER="aes_gcm_ncipher:ffb944993c73a1d3"

# return sha256sum of string that matches pattern in subzero_bin
function find_gateway_digest {
    pattern="$1"
    subzero_bin="$2"
    ret=$(strings "$subzero_bin" | \
          grep -Eo "${pattern}[[:alnum:]]*" | \
          tr -d '\n' | \
          sha256sum | \
          cut -f1 -d ' ')
    echo "$ret"
}

# print "TESTNET", "MAINNET", or "UNKNOWN" as gateway type
function print_gateway_address_type {
    subzero_bin="$1"

    # Check for testnet gateway
    if [ "$GATEWAY_TESTNET_SHA256SUM" \
          = $(find_gateway_digest "tpubDA3z" "$subzero_bin") ]; then
      testnet=1
    else
      testnet=0
    fi

    # Check for mainnet gateway
    if [ "$GATEWAY_MAINNET_SHA256SUM" \
          = $(find_gateway_digest "xpub68it" "$subzero_bin") ]; then
      mainnet=1
    else
      mainnet=0
    fi

    count=$(( $testnet + $mainnet ))
    if [ $count != 1 ]; then
      echo "GATEWAY TYPE: UNKNOWN"
      if [ $count == 0 ]; then
        >&2 echo "No gateway address detected"
      else
        >&2 echo "More than one gateway address detected"
      fi
      return 0
    fi

    if [ "x$testnet" == "x1" ]; then
      echo "GATEWAY TYPE: TESTNET"
      return 0
    fi

    if [ "x$mainnet" == "x1" ]; then
      echo "GATEWAY TYPE: MAINNET"
      return 0
    fi

   # Should never get here
   exit 1
}


# print "DEV" or "NCIPHER" as AES key type
function print_aes_key_type {
  subzero_bin="$1"

  if grep -q "$MAGIC_DEV" $subzero_bin; then
    echo "AES KEY TYPE: DEV"
  elif grep -q "$MAGIC_NCIPHER" $subzero_bin; then
    echo "AES KEY TYPE: NCIPHER"
  else
    echo "AES KEY TYPE: UNKNOWN"
  fi

  return 0
}

print_gateway_address_type "$1"
print_aes_key_type "$1"