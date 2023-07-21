cmake_minimum_required (VERSION 2.8)
project (subzero)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -pedantic -Wextra -Wwrite-strings -Wstrict-prototypes -Wmissing-prototypes -Wcast-qual -std=gnu11 -DPB_FIELD_16BIT")

# Part 1/2 of magic to drop the full path from __FILE__
string(LENGTH "${CMAKE_SOURCE_DIR}/" SOURCE_PATH_SIZE)
add_definitions("-DSOURCE_PATH_SIZE=${SOURCE_PATH_SIZE}")

option(ENABLE_ASAN "Enable Address Sanitizer" OFF) # enable with -DENABLE_ASAN=ON
option(ENABLE_MSAN "Enable Uninitialized Memory Sanitizer" OFF) # enable with -DENABLE_MSAN=ON
option(ENABLE_UBSAN "Enable Undefined Behavior Sanitizer" OFF) # enable with -DENABLE_UBSAN=ON
option(ENABLE_COVERAGE "Enable code coverage" OFF) # enable with -DENABLE_COVERAGE=ON
option(ENABLE_FUZZER "Enable fuzzer via LLVM's libfuzzer" OFF) # enable with -DENABLE_FUZZER=ON

if ($ENV{TARGET} MATCHES "nCipher")
  message("Building for nCipher(powerpc32) architecture.")
  include(codesafe.cmake)

  if ($ENV{UNSIGNED} MATCHES "yes")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DUNSIGNED")
  endif ()
elseif($ENV{TARGET} MATCHES "dev")
  message("Building for the host architecture")
else ()
  message(FATAL_ERROR "Unsupported TARGET value.")
endif ()

# Note that in order for ASAN/MSAN and UBSAN to be enabled in subdirectories
# (e.g. trezor-crypto, external-crypto, proto, etc), the logic that enables
# it has to run before the corresponding add_subdirectory() calls.
if (ENABLE_ASAN AND ENABLE_MSAN)
  message(FATAL_ERROR "ASAN and MSAN are mutually exclusive")
endif ()

if (ENABLE_ASAN)
  message(STATUS "Enabling ASAN")
  list(APPEND FSANITIZE "address")
  list(APPEND EXTRA_SANITIZER_FLAGS "-fno-omit-frame-pointer")
endif ()

# Note that as of April 2023, MSAN requires Clang and works on x86_64 but not on arm64.
if (ENABLE_MSAN)
  if (NOT CMAKE_C_COMPILER_ID MATCHES "Clang")
    message(FATAL_ERROR "MSAN only works with Clang")
  endif ()
  message(STATUS "Enabling MSAN")
  list(APPEND FSANITIZE "memory")
  list(APPEND EXTRA_SANITIZER_FLAGS "-fno-omit-frame-pointer")
endif ()

if (ENABLE_UBSAN)
  message(STATUS "Enabling UBSAN")
  list(APPEND FSANITIZE "undefined")
  list(APPEND EXTRA_SANITIZER_FLAGS "-fno-sanitize-recover=undefined")
endif ()

if (ENABLE_FUZZER)
  # Note that Apple does NOT ship a full Clang toolchain on MacOS (tested on 13.4.1)!
  # In order to get libfuzzer to link properly, you need to install the full LLVM toolchain
  # with homebrew, then switch your environment to use the homebrew versions of clang for
  # building and re-run cmake. It's not recommended to do this globally, as using a 3rd party
  # clang for everything can break your system, so just do it in a single shell - consider
  # writing a script which does this.
  # On my machine this looks like:
  #   export PATH="/opt/homebrew/opt/llvm/bin:$PATH"
  #   export LDFLAGS="-L/opt/homebrew/opt/llvm/lib/c++ -Wl,-rpath,/opt/homebrew/opt/llvm/lib/c++ -L/opt/homebrew/opt/llvm/lib"
  #   export CFLAGS="-I/opt/homebrew/opt/llvm/include"
  #   export CPPFLAGS="-I/opt/homebrew/opt/llvm/include"
  #   export CXXFLAGS="-I/opt/homebrew/opt/llvm/include"
  #   export CC=`which clang`
  #   export CXX=`which clang`
  if (NOT CMAKE_C_COMPILER_ID MATCHES "Clang")
    message(FATAL_ERROR "LLVM's libfuzzer requires Clang")
  endif ()
  message(STATUS "Enabling fuzzing via LLVM's libfuzzer")
  list(APPEND FSANITIZE "fuzzer")
  list(APPEND EXTRA_SANITIZER_FLAGS "-g") # fuzzing needs debug symbols
  list(APPEND EXTRA_SANITIZER_FLAGS "-O1") # make fuzzer faster
  set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DFUZZING_BUILD_MODE_UNSAFE_FOR_PRODUCTION")
endif ()

if (ENABLE_ASAN OR ENABLE_MSAN OR ENABLE_UBSAN OR ENABLE_FUZZER)
  string(REPLACE ";" "," FSANITIZE_STR "${FSANITIZE}")
  add_compile_options(-fsanitize=${FSANITIZE_STR} ${EXTRA_SANITIZER_FLAGS})
  add_link_options(-fsanitize=${FSANITIZE_STR})
endif ()

if (ENABLE_COVERAGE)
  message(STATUS "Enabling code coverage")
  if (CMAKE_C_COMPILER_ID MATCHES "Clang") # "Clang" or "AppleClang"
    # To generate a coverage report from running './subzero --checks-only' using clang:
    #   cd <subzero root>/core/build
    #   cmake as normal and add -DENABLE_COVERAGE=ON
    #   make
    #   LLVM_PROFILE_FILE="subzero.profraw" ./subzero --checks-only
    #   llvm-profdata merge -sparse subzero.profraw -o subzero.profdata
    #   llvm-cov show ./subzero -instr-profile=subzero.profdata -format=html -output-dir=coverage ../{src,include}
    #   open coverage/index.html
    # Note that coverage can be combined with ASAN and/or UBSAN if desired.
    # See more details at: https://clang.llvm.org/docs/SourceBasedCodeCoverage.html
    #
    # Note that Apple does NOT ship a full Clang toolchain on MacOS (tested on 13.3.1)!
    # In order to get the llvm-profdata and llvm-cov command line tools, you need to install
    # the full LLVM toolchain with homebrew, then switch your environment to use the homebrew
    # versions of clang for building and re-run cmake. It's not recommended to do this
    # globally, as using a 3rd party clang for everything can break your system, so just do
    # it in a single shell - consider writing a script which does this.
    # On my machine this looks like:
    #   export PATH="/opt/homebrew/opt/llvm/bin:$PATH"
    #   export LDFLAGS="-L/opt/homebrew/opt/llvm/lib/c++ -Wl,-rpath,/opt/homebrew/opt/llvm/lib/c++ -L/opt/homebrew/opt/llvm/lib"
    #   export CFLAGS="-I/opt/homebrew/opt/llvm/include"
    #   export CPPFLAGS="-I/opt/homebrew/opt/llvm/include"
    #   export CXXFLAGS="-I/opt/homebrew/opt/llvm/include"
    #   export CC=`which clang`
    #   export CXX=`which clang`
    #
    # Note: on Darwin (i.e. MacOS) it's possible to collect code coverage even when the process is killed by a signal.
    # This *might* also work on Linux, but it's only confirmed to work on Darwin according to clang documentation,
    # as of May 2023.
    #
    # This allows us to get code coverage of code paths exercised by the GUI tests (e.g. gui-...-.jar --signtx-test):
    # In terminal 1:
    #   LLVM_PROFILE_FILE="subzero.profraw%c" ./subzero
    # In terminal 2:
    #   run the GUI test (e.g. java -jar java/gui/build/libs/gui-1.0.0-SNAPSHOT-shaded.jar --signtx-test)
    # Can also run additional tests in terminal 2, e.g. for wallet creation.
    # Back to terminal 1:
    #   ctrl-C to kill the subzero process
    #   llvm-profdata merge -sparse subzero.profraw -o subzero.profdata
    #   llvm-cov show ./subzero -instr-profile=subzero.profdata -format=html -output-dir=coverage ../{src,include}
    #   open coverage/index.html
    add_compile_options(-fprofile-instr-generate -fcoverage-mapping -O0 -g)
    add_link_options(-fprofile-instr-generate)
  elseif (CMAKE_C_COMPILER_ID STREQUAL "GNU") # GCC
    # To generate a coverage report from running './subzero --checks-only' using gcc:
    #   cd <subzero root>/core/build
    #   cmake as normal and add -DENABLE_COVERAGE=ON
    #   make
    #   ./subzero --checks-only
    #   lcov --capture --directory=./ --output-file coverage.info
    #   genhtml coverage.info --output-directory coverage
    #   open coverage/index.html
    # Note that coverage can be combined with ASAN and/or UBSAN if desired.
    #
    # Note that you may need to install the lcov and/or genhtml binaries using your system's
    # package manager.
    # On my Xubuntu system this was done with 'sudo apt install lcov'.
    add_compile_options(-coverage -O0 -g)
    add_link_options(-coverage)
  else ()
    # MSVC / Intel CC / others not supported since we don't currently use them.
    message(FATAL_ERROR "Code coverage not supported for your C compiler")
  endif ()
endif ()

add_subdirectory(trezor-crypto)
add_subdirectory(external-crypto)
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
    "src/qrsignatures.c"
)

set(checks_SRC
    "src/checks/bip32.c"
    "src/checks/check_sign_tx.c"
    "src/checks/conv_checks.c"
    "src/checks/misc_checks.c"
    "src/checks/rpc_checks.c"
    "src/checks/self_checks.c"
    "src/checks/validate_fees.c"
    "src/checks/verify_mix_entropy.c"
    "src/checks/verify_protect_pubkey.c"
    "src/checks/verify_no_rollback.c"
    "src/checks/wycheproof_checks.c"
)
list(APPEND main_SRC ${checks_SRC})

set(include_SRC
    "include/aes_gcm_common.h"
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
      "include/aes_gcm_ncipher.h"
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
      "include/aes_gcm_dev.h"
      "src/dev/additional_checks.c"
      "src/dev/no_rollback.c"
      "src/dev/execute_command_hooks.c"
      "src/dev/init.c"
      "src/dev/init_wallet.c"
      "src/dev/protection.c"
  )
  list(APPEND main_SRC ${extra_SRC})

  # We need to build a small static libary to avoid conflicting headers
  add_library(aes_gcm_dev STATIC "src/dev/aes_gcm.c")
  target_include_directories(aes_gcm_dev PUBLIC
      "include"
      "external-crypto/modes"
      "external-crypto/aes"
      "nanopb"
      "${CMAKE_BINARY_DIR}/proto"
  )
  target_link_libraries(aes_gcm_dev SubzeroProtos)

endif ()

if (ENABLE_FUZZER)
  list(APPEND main_SRC "src/fuzz.c")
  set(BINARY_NAME subzero_fuzzer)
else ()
  list(APPEND main_SRC "src/main.c")
  set(BINARY_NAME subzero)
endif ()

add_executable(${BINARY_NAME} ${main_SRC})

if ($ENV{TARGET} MATCHES "nCipher")
  target_link_libraries(${BINARY_NAME} TrezorCrypto SubzeroProtos)
else()
  target_link_libraries(${BINARY_NAME} TrezorCrypto SubzeroProtos aes_gcm_dev GladmanGCM)
endif ()
