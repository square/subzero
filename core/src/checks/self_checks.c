#include <stdio.h>

#include "checks.h"
#include "log.h"

/**
 * Run all the self checks.
 *
 * Self checks continue running even if one of them fails. It is however
 * recommended to order them from simplest to more complicated.
 *
 * Returns -1 if any test failed.
 */
int run_self_checks() {
  int t, r = 0;

  // environment specific initialization
  r = pre_run_self_checks();
  if (r != 0) {
    ERROR("pre_run_self_checks failed.");
    return r;
  }

  t = verify_mix_entropy();
  if (t != 0) {
    r = -1;
    ERROR("verify_mix_entropy failed.");
  }

  t = verify_protect_pubkey();
  if (t != 0) {
    r = -1;
    ERROR("verify_protect_pubkey failed.");
  }

  t = verify_bip32();
  if (t != 0) {
    r = -1;
    ERROR("self check failure: verify_bip32() failed.");
  }

  t = verify_sign_tx();
  if (t != 0) {
    r = -1;
    ERROR("self check failure: verify_sign_tx failed.");
  }

  t = verify_validate_fees();
  if (t != 0) {
    r = -1;
    ERROR("self check failure: verify_validate_fees failed.");
  }

  t = verify_no_rollback();
  if (t != 0) {
    r = -1;
    ERROR("self check failure: verify_no_rollback failed.");
  }

  // environment specific additional checks + cleanup
  t = post_run_self_checks();
  if (t != 0) {
    r = -1;
    ERROR("post_run_self_checks failed.");
  }
  return r;
}
