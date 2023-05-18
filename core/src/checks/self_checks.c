#include "checks.h"
#include "log.h"

#include <stdio.h>
#include <stdlib.h>

typedef int (*self_check_function)(void);

typedef struct self_check_function_info {
  self_check_function func;
  const char* name;
} self_check_function_info;

#define MAX_SELF_CHECKS ((size_t) 128)
static self_check_function_info self_checks[MAX_SELF_CHECKS] = { { 0 } };
static size_t self_checks_count = 0;
static bool self_checks_registered = false;

static void register_self_check(self_check_function func, const char* name) {
  if (NULL == func || NULL == name) {
    ERROR("%s: func or name is NULL. Cannot continue.", __func__);
    abort();
  }
  if (self_checks_count >= MAX_SELF_CHECKS) {
    ERROR("%s: too many registered self checks. Increase MAX_SELF_CHECKS and recompile", __func__);
    abort();
  }
  self_checks[self_checks_count].func = func;
  self_checks[self_checks_count].name = name;
  self_checks_count++;
}

// For each new self check function added, this constant needs to be incremented by 1.
#define EXPECTED_SELF_CHECKS_COUNT ((size_t) 12)

#define REGISTER_SELF_CHECK(name) register_self_check(name, #name)

/**
 * When new self check functions are added, add them here. Checks run in registration order.
 */
static void register_all_self_checks(void) {
  if (self_checks_registered) {
    return;
  }

  REGISTER_SELF_CHECK(verify_byte_order);
  REGISTER_SELF_CHECK(verify_u16_to_little_endian_bytes);
  REGISTER_SELF_CHECK(verify_u32_to_little_endian_bytes);
  REGISTER_SELF_CHECK(verify_mix_entropy);
  REGISTER_SELF_CHECK(verify_protect_pubkey);
  REGISTER_SELF_CHECK(verify_bip32);
  REGISTER_SELF_CHECK(verify_sign_tx);
  REGISTER_SELF_CHECK(verify_check_qrsignature_pub);
  REGISTER_SELF_CHECK(verify_validate_fees);
  REGISTER_SELF_CHECK(verify_no_rollback);
  REGISTER_SELF_CHECK(verify_conv_btc_to_satoshi);
  REGISTER_SELF_CHECK(verify_rpc_oversized_message_rejected);

  if (EXPECTED_SELF_CHECKS_COUNT != self_checks_count) {
    ERROR(
        "%s: EXPECTED_SELF_CHECKS_COUNT (%zu) != self_checks_count (%zu). "
        "You probably registered a new self check and forgot to increment EXPECTED_SELF_CHECKS_COUNT. "
        "Do so now and recompile.",
        __func__,
        EXPECTED_SELF_CHECKS_COUNT,
        self_checks_count);
    abort();
  }

  self_checks_registered = true;
}

/**
 * Run all the self checks.
 *
 * Self checks continue running even if one of them fails. It is however
 * recommended to order them from simplest to more complicated.
 *
 * Returns -1 if any test failed.
 */
int run_self_checks(void) {
  int r = 0;

  register_all_self_checks();

  // environment specific initialization
  r = pre_run_self_checks();
  if (r != 0) {
    ERROR("pre_run_self_checks failed.");
    return r;
  }

  for (size_t i = 0; i < self_checks_count; ++i) {
    int t = self_checks[i].func();
    if (t != 0) {
      r = -1;
      ERROR("self check failure: %s failed. rc = %d", self_checks[i].name, t);
    }
  }

  // environment specific additional checks + cleanup
  int t = post_run_self_checks();
  if (t != 0) {
    r = -1;
    ERROR("post_run_self_checks failed.");
  }
  return r;
}
