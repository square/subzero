cmake_minimum_required(VERSION 2.8)

set(SOURCES
    address.c
    # Note: we don't use the trezor AES implementation in subzero.
    # aes/aes_modes.c
    # aes/aescrypt.c
    # aes/aeskey.c
    # aes/aestab.c
    # aes/aestst.c
    base32.c
    base58.c
    bignum.c
    bip32.c
    bip39_english.c
    bip39.c
    blake256.c
    blake2b.c
    blake2s.c
    # cardano.c  # Note: not sure what "cardano" is, but we don't use in in subzero.
    # cash_addr.c  # Note: not sure what "cash_addr" is, but we don't use in in subzero.

    # Note: we don't use the trezor ChaCha20 implementation in subzero.
    # chacha_drbg.c
    # chacha20poly1305/chacha_merged.c
    # chacha20poly1305/chacha20poly1305.c
    # chacha20poly1305/poly1305-donna.c
    # chacha20poly1305/rfc7539.c
    curves.c
    ecdsa.c
    ed25519-donna/curve25519-donna-32bit.c
    ed25519-donna/curve25519-donna-helpers.c
    ed25519-donna/curve25519-donna-scalarmult-base.c
    ed25519-donna/ed25519-donna-32bit-tables.c
    ed25519-donna/ed25519-donna-basepoint-table.c
    ed25519-donna/ed25519-donna-impl-base.c
    ed25519-donna/ed25519-keccak.c
    ed25519-donna/ed25519-sha3.c
    ed25519-donna/ed25519.c
    ed25519-donna/modm-donna-32bit.c
    # fuzzer/fuzzer.c  # Note: we don't include the trezor fuzzer in our prod binary.
    groestl.c
    hasher.c
    hmac_drbg.c
    hmac.c
    memzero.c
    # Note: we don't use or need monero functionality
    # monero/base58.c
    # monero/serialize.c
    # monero/xmr.c

    # nem.c  # Note: not sure what "nem" is, but we don't use it in subzero.
    nist256p1.c
    pbkdf2.c
    rand.c
    # rc4.c  # Note: we don't use rc4 in subzero.
    rfc6979.c
    ripemd160.c
    script.c
    secp256k1.c
    # Note: not sure what "segwit_addr" is, but we don't use it in subzero.
    # segwit_addr.c
    sha2.c
    sha3.c
    # Note: we don't use shamir's secret sharing in subzero.
    # shamir.c
    # # Note: not sure what "slip39" is, but we don't use it in subzero.
    # slip39.c

    # Note: we don't include the trezor tests, can't have multiple main() functions.
    # tests/test_check.c
    # tests/test_openssl.c
    # tests/test_speed.c

    # Note: we don't include the trezor tools, can't have multiple main() functions.
    # tools/bip39bruteforce.c
    # tools/mktable.c
    # tools/xpubaddrgen.c

    # Note: we don't use the ZKP functionality in subzero.
    # zkp_bip340.c
    # zkp_context.c
    # zkp_ecdsa.c
  )

add_library(TrezorCrypto STATIC ${SOURCES})

target_include_directories(TrezorCrypto PUBLIC .)
# target_include_directories(TrezorCrypto PUBLIC aes)
# target_include_directories(TrezorCrypto PUBLIC chacha20poly1305)
target_include_directories(TrezorCrypto PUBLIC ed25519-donna)

target_compile_options(TrezorCrypto PRIVATE "-std=gnu11")
add_definitions(-Wno-cast-qual -Wno-missing-prototypes)

if(MSVC)
  set_source_files_properties(${SOURCES} PROPERTIES LANGUAGE CXX)
endif(MSVC)

# Build trezor-crypto's test_openssl (OpenSSL library)
if (TREZOR_CRYPTO_TESTS)
    find_package(OpenSSL)
    if (NOT OPENSSL_FOUND)
      message(FATAL_ERROR "Could not find OpenSSL. Make sure it's installed, and possibly set the OPENSSL_ROOT_DIR env var")
    endif ()

    add_executable(test_openssl tests/test_openssl.c)
    target_include_directories(test_openssl PRIVATE "${OPENSSL_INCLUDE_DIR}")
    target_link_libraries(test_openssl TrezorCrypto pthread m "${OPENSSL_CRYPTO_LIBRARY}")
    add_test(NAME trezor-crypto-openssl COMMAND test_openssl 100)
endif()
