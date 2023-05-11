#pragma once

#include <squareup/subzero/internal.pb.h>

/**
 * Performs self checks before accepting any requests.
 *
 * Returns 0 on success. If any of these tests fail, the code should abort.
 */
int run_self_checks(void);

/**
 * Called before the first self check runs. Enables us to create a test key.
 *
 * Returns 0 on success.
 */
int pre_run_self_checks(void);

/**
 * Called after the last self check runs. Enables running environment specific
 * tests and perform cleanup.
 *
 * Returns 0 on success.
 */
int post_run_self_checks(void);

/**
 * Checks if the machine is little-endian or big-endian, and verifies that the
 * result matches the BYTE_ORDER preprocessor constant.
 */
int verify_byte_order(void);

int verify_bip32(void);
int verify_sign_tx(void);
int verify_validate_fees(void);
int verify_mix_entropy(void);
int verify_protect_pubkey(void);
int verify_no_rollback(void);
int verify_check_qrsignature_pub(void);
int verify_conv_btc_to_satoshi(void);

/**
 * Verifies that calling handle_incoming_message() with a serialized protobuf
 * message which exceeds nanopb field size limits (defined in proto .options
 * files) fails as expected.
 *
 * Note that as this function uses statically-allocated buffers, it is not thread-safe.
 */
int verify_rpc_oversized_message_rejected(void);

#define ASSERT_STR_EQUAL(value, expecting, message) \
  do {                                              \
    if (strcmp(expecting, value) != 0) {            \
      ERROR(message);                               \
      ERROR("expecting: %s", expecting);            \
      ERROR("got: %s", value);                      \
      return -1;                                    \
    }                                               \
  } while(0)
