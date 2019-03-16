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

int verify_bip32(void);
int verify_sign_tx(void);
int verify_validate_fees(void);
int verify_mix_entropy(void);
int verify_protect_pubkey(void);
int verify_no_rollback(void);

#define ASSERT_STR_EQUAL(value, expecting, message) \
  do {                                              \
    if (strcmp(expecting, value) != 0) {            \
      ERROR(message);                               \
      ERROR("expecting: %s", expecting);            \
      ERROR("got: %s", value);                      \
      return -1;                                    \
    }                                               \
  } while(0)
