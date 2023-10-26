#include "checks.h"
#include "log.h"

#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>

typedef int (*self_check_function)(void);

typedef struct self_check_function_info {
  self_check_function func;
  const char* name;
  int result;
  bool attempted;
} self_check_function_info;

// For each new self check function added, this constant needs to be incremented by 1.
#define MAX_SELF_CHECKS ((size_t) 14)
static self_check_function_info self_checks[MAX_SELF_CHECKS] = { { 0 } };
static size_t self_checks_count = 0;
static bool self_checks_registered = false;

static void register_self_check(self_check_function func, const char* name) {
  if (NULL == func || NULL == name) {
    FATAL("%s: func or name is NULL. Cannot continue.", __func__);
  }
  if (self_checks_count >= MAX_SELF_CHECKS) {
    FATAL(
        "%s: too many registered self checks. You probably registered a new self check "
        "and forgot to increment MAX_SELF_CHECKS. Do so now and recompile.",
        __func__);
  }
  self_checks[self_checks_count].func = func;
  self_checks[self_checks_count].name = name;
  self_checks_count++;
}

#define REGISTER_SELF_CHECK(name) register_self_check(name, #name)

/**
 * When new self check functions are added, add them here. Checks run in registration order.
 */
static void register_all_self_checks(void) {
  if (self_checks_registered) {
    return;
  }

  REGISTER_SELF_CHECK(pre_run_self_checks); // NOTE: this MUST always be registered first
  REGISTER_SELF_CHECK(verify_byte_order);
  REGISTER_SELF_CHECK(verify_mix_entropy);
  REGISTER_SELF_CHECK(verify_protect_pubkey);
  REGISTER_SELF_CHECK(verify_bip32);
  REGISTER_SELF_CHECK(verify_sign_tx);
  REGISTER_SELF_CHECK(verify_check_qrsignature_pub);
  REGISTER_SELF_CHECK(verify_validate_fees);
  REGISTER_SELF_CHECK(verify_no_rollback);
  REGISTER_SELF_CHECK(verify_no_rollback_write_to_buf);
  REGISTER_SELF_CHECK(verify_conv_btc_to_satoshi);
  REGISTER_SELF_CHECK(verify_wycheproof);
  REGISTER_SELF_CHECK(verify_rpc_oversized_message_rejected);
  REGISTER_SELF_CHECK(post_run_self_checks); // NOTE: this MUST always be registered last

  if (MAX_SELF_CHECKS != self_checks_count) {
    FATAL(
        "%s: MAX_SELF_CHECKS (%zu) != self_checks_count (%zu). "
        "You probably removed a self check and forgot to decrement MAX_SELF_CHECKS. "
        "Do so now and recompile.",
        __func__,
        MAX_SELF_CHECKS,
        self_checks_count);
  }

  if (self_checks[0].func != &pre_run_self_checks) {
    FATAL("%s: first registered self check must be pre_run_self_checks", __func__);
  }

  if (self_checks[self_checks_count - 1].func != &post_run_self_checks) {
    FATAL("%s: last registered self check must be post_run_self_checks", __func__);
  }

  self_checks_registered = true;
}

static void print_self_check_results(void) {
  INFO("===== SELF CHECKS SUMMARY =====");
  for (size_t i = 0; i < self_checks_count; ++i) {
    const struct self_check_function_info* info = &self_checks[i];
    if (info->attempted) {
      if (0 == info->result) {
        INFO("%s: succeeded", info->name);
      } else {
        ERROR("%s: failed with result %d", info->name, info->result);
      }
    } else {
      ERROR("%s: did not run", info->name);
    }
  }
  INFO("===== END SELF CHECKS SUMMARY =====");
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
  bool success = true;

  register_all_self_checks();

  for (size_t i = 0; i < self_checks_count; ++i) {
    struct self_check_function_info* info = &self_checks[i];
    int check_result = info->func();
    info->attempted = true;
    info->result = check_result;
    if (check_result != 0) {
      success = false;
      ERROR("self check failure: %s failed with result %d", info->name, check_result);

      // if pre_run_self_checks() fails, we cannot continue
      if (0 == i) {
        break;
      }
    } else {
      INFO("%s: ok", info->name);
    }
  }

  if (!success) {
    print_self_check_results();
  }
  return success ? 0 : -1;
}
