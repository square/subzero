#include "checks.h"
#include "log.h"
#include "no_rollback.h"

int verify_no_rollback() {
  char verify_file[] = "selfcheck";
  Result r;
  r = no_rollback_write_version(verify_file, 1, 1);
  if (r != Result_SUCCESS) {
    ERROR("verify_no_rollback: no_rollback_write_version failed: %d", r);
    return -1;
  }

  ERROR("(next line is expected to show red text...)");
  r = no_rollback_check(verify_file, true, 0, 1); // should fail with incorrect magic
  if (r != Result_NO_ROLLBACK_INVALID_MAGIC) {
    ERROR("verify_no_rollback: expecting incorrect magic, got %d", r);
    return -1;
  }

  ERROR("(next line is expected to show red text...)");
  r = no_rollback_check(verify_file, true, 1, 0); // should fail with rollback detected
  if (r != Result_NO_ROLLBACK_INVALID_VERSION) {
    ERROR("verify_no_rollback: expecting incorrect version (1), got %d", r);
    return -1;
  }

  r = no_rollback_check(verify_file, true, 1, 1); // should succeed
  if (r != Result_SUCCESS) {
    ERROR("verify_no_rollback: expecting success (1), got: %d", r);
    return -1;
  }

  r = no_rollback_check(verify_file, true, 1, 1); // should succeed
  if (r != Result_SUCCESS) {
    ERROR("verify_no_rollback: expecting success (2), got: %d", r);
    return -1;
  }

  r = no_rollback_check(verify_file, true, 1, 2); // should succeed
  if (r != Result_SUCCESS) {
    ERROR("verify_no_rollback: expecting success (3), got: %d", r);
    return -1;
  }

  r = no_rollback_check(verify_file, true, 1, 2); // should succeed
  if (r != Result_SUCCESS) {
    ERROR("verify_no_rollback: expecting success (4), got: %d", r);
    return -1;
  }

  ERROR("(next line is expected to show red text...)");
  r = no_rollback_check(verify_file, true, 1, 1); // should fail with rollback detected
  if (r != Result_NO_ROLLBACK_INVALID_VERSION) {
    ERROR("verify_no_rollback: expecting incorrect version (2), got %d", r);
    return -1;
  }

  INFO("verify_no_rollback: ok");
  return 0;
}
