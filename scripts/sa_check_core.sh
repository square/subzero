#!/usr/bin/env bash

# clang-check (included in LLVM tools) needs to be installed for 
# static analysis on subzero CORE a la clang static analyzer

set -euxo pipefail

echo "Static analysis through clang static analyzer"
EXCLUDE_PATH_PATTERN="\/external-crypto\/\|\/trezor-crypto\/"
SUBZERO_ROOT=$(cd "$(dirname "$0")"; pwd -P)/..

${SUBZERO_ROOT}/scripts/build_core.sh

COMPILE_DB_DIR="${SUBZERO_ROOT}/core/build"
COMPILE_DB="${COMPILE_DB_DIR}/compile_commands.json"
if [ ! -f "${COMPILE_DB}" ]; then
    exit 1
fi

grep file "${COMPILE_DB}" |
awk '{ print $2; }' |
sed 's/\"//g' |
sed 's/,//g' |
while read FILE; do
  if ! grep -q ${EXCLUDE_PATH_PATTERN} <<< "${FILE}"; then
    cd $(dirname ${FILE});
    output=$(clang-check -analyze -extra-arg -Xanalyzer -extra-arg \
      -analyzer-disable-checker=unix \
      -extra-arg -Xanalyzer -extra-arg \
      -analyzer-checker=core,deadcode,security,cplusplus,nullability \
      -extra-arg -Xanalyzer -extra-arg \
      -analyzer-disable-checker=security.insecureAPI.DeprecatedOrUnsafeBufferHandling \
      -extra-arg -Xanalyzer -extra-arg -analyzer-output=text \
      -p ${COMPILE_DB_DIR} \
      $(basename ${FILE}) 2>&1)
    if grep -q "warning:" <<< "${output}"; then
      >&2 echo ${output}
      exit 1
    fi
  fi
done
echo "Static analysis returns clean result"
