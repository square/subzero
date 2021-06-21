#!/usr/bin/env bash
set -euxo pipefail

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 /path/to/check_dir"
    exit 1
fi

CHECK_DIR="$1"
SCRIPT_ROOT=$(cd "$(dirname "$0")"; pwd -P)
EXPECTED_OUTPUT_DEV_TESTNET="GATEWAY TYPE: TESTNET\nAES KEY TYPE: DEV"
EXPECTED_OUTPUT_DEV_MAINNET="GATEWAY TYPE: MAINNET\nAES KEY TYPE: DEV"

function check_output {
  subzero_bin="$1"
  expected_output="$2"

  output_digest=$(${SCRIPT_ROOT}/binary_static_analysis.sh ${subzero_bin} \
    | sha256sum | cut -f1 -d' ')
  expected_output_digest=$(echo -e "${expected_output}" \
    | sha256sum | cut -f1 -d' ')

  if [[ $output_digest != $expected_output_digest ]]; then
          echo "Binary static analysis failed on ${subzero_bin}"
          exit 1
  fi
}

# Run binary static analysis on dev-testnet and dev-mainnet targets
check_output "${CHECK_DIR}/subzero-testnet" "$EXPECTED_OUTPUT_DEV_TESTNET"
check_output "${CHECK_DIR}/subzero-mainnet" "$EXPECTED_OUTPUT_DEV_MAINNET"