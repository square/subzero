cmake_minimum_required (VERSION 2.8)
project (subzero)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -pedantic -Wextra -Wwrite-strings -Wstrict-prototypes -Wmissing-prototypes -Wcast-qual -std=gnu11 -DPB_FIELD_16BIT")

# Part 1/2 of magic to drop the full path from __FILE__
string(LENGTH "${CMAKE_SOURCE_DIR}/" SOURCE_PATH_SIZE)
add_definitions("-DSOURCE_PATH_SIZE=${SOURCE_PATH_SIZE}")

if ($ENV{TARGET} MATCHES "nCipher")
  include(codesafe.cmake)

  if ($ENV{UNSIGNED} MATCHES "yes")
     set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DUNSIGNED")
  endif ()
endif ()

add_subdirectory(trezor-crypto)
add_subdirectory(proto)

include_directories(include nanopb trezor-crypto "${CMAKE_BINARY_DIR}/proto")

# Main source files
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Werror -Wno-unknown-pragmas")

if ($ENV{CURRENCY} MATCHES "btc-testnet")
  set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DBTC_TESTNET")
elseif ($ENV{CURRENCY} MATCHES "btc-mainnet")
  set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DBTC_MAINNET")
endif ()

set(main_SRC
    "src/conv.c"
    "src/finalize_wallet.c"
    "src/hash.c"
    "src/init_wallet.c"
    "src/memzero.c"
    "src/nanopb_stream.c"
    "src/no_rollback.c"
    "src/print.c"
    "src/rpc.c"
    "src/script.c"
    "src/sign.c"
)

set(checks_SRC
    "src/checks/bip32.c"
    "src/checks/check_sign_tx.c"
    "src/checks/self_checks.c"
    "src/checks/validate_fees.c"
    "src/checks/verify_mix_entropy.c"
    "src/checks/verify_protect_pubkey.c"
    "src/checks/verify_no_rollback.c")
list(APPEND main_SRC ${checks_SRC})

set(include_SRC
    "include/aes_gcm.h"
    "include/checks.h"
    "include/config.h"
    "include/conv.h"
    "include/hash.h"
    "include/init_wallet.h"
    "include/init.h"
    "include/log.h"
    "include/memzero.h"
    "include/module_certificate.h"
    "include/nanopb_stream.h"
    "include/no_rollback.h"
    "include/print.h"
    "include/protection.h"
    "include/rpc.h"
    "include/script.h"
    "include/sign.h"
)
list(APPEND main_SRC ${include_SRC})

if ($ENV{TARGET} MATCHES "nCipher")
  set(extra_SRC
      "src/ncipher/additional_checks.c"
      "src/ncipher/aes_gcm.c"
      "src/ncipher/execute_command_hooks.c"
      "src/ncipher/init_wallet.c"
      "src/ncipher/init.c"
      "src/ncipher/module_certificate.c"
      "src/ncipher/no_rollback.c"
      "src/ncipher/protection.c"
      "src/ncipher/transact.c"
  )
  list(APPEND main_SRC ${extra_SRC})
else ()
  set(extra_SRC
      "src/dev/additional_checks.c"
      "src/dev/no_rollback.c"
      "src/dev/execute_command_hooks.c"
      "src/dev/init.c"
      "src/dev/init_wallet.c"
      "src/dev/protection.c"
  )
  list(APPEND main_SRC ${extra_SRC})
endif ()

if ($ENV{TARGET} MATCHES "fuzz")
  list(APPEND main_SRC "src/fuzz.c")
else ()
  list(APPEND main_SRC "src/main.c")
endif ()

add_executable(subzero ${main_SRC})
target_link_libraries(subzero TrezorCrypto SubzeroProtos)
