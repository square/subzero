#include "checks.h"
#include "config.h"
#include "log.h"
#include "no_rollback.h"
#include "squareup/subzero/internal.pb.h"

#include <assert.h>
#include <stdbool.h>

int verify_no_rollback(void) {
  static_assert(VERSION > 0, "Version must not be 0");
  const uint32_t PREV_VERSION = VERSION - 1;
  const uint32_t NEXT_VERSION = VERSION + 1;

  // Use a different no_rollback magic constant for tests.
  const uint32_t TEST_VERSION_MAGIC = VERSION_MAGIC + 1;

  const char verify_file[] = "selfcheck01";
  Result r;
  r = no_rollback_write_version(verify_file, TEST_VERSION_MAGIC, VERSION);
  if (r != Result_SUCCESS) {
    ERROR("verify_no_rollback: no_rollback_write_version failed: %d", r);
    return -1;
  }

  ERROR("(next line is expected to show red text...)");
  r = no_rollback_check(verify_file, true, VERSION_MAGIC, VERSION); // should fail with incorrect magic
  if (r != Result_NO_ROLLBACK_INVALID_MAGIC) {
    ERROR("verify_no_rollback: expecting incorrect magic, got %d", r);
    return -1;
  }

  ERROR("(next line is expected to show red text...)");
  r = no_rollback_check(verify_file, true, TEST_VERSION_MAGIC, PREV_VERSION); // should fail with rollback detected
  if (r != Result_NO_ROLLBACK_INVALID_VERSION) {
    ERROR("verify_no_rollback: expecting incorrect version (1), got %d", r);
    return -1;
  }

  r = no_rollback_check(verify_file, true, TEST_VERSION_MAGIC, VERSION); // should succeed
  if (r != Result_SUCCESS) {
    ERROR("verify_no_rollback: expecting success (1), got: %d", r);
    return -1;
  }

  r = no_rollback_check(verify_file, true, TEST_VERSION_MAGIC, VERSION); // should succeed
  if (r != Result_SUCCESS) {
    ERROR("verify_no_rollback: expecting success (2), got: %d", r);
    return -1;
  }

  r = no_rollback_check(verify_file, true, TEST_VERSION_MAGIC, NEXT_VERSION); // should succeed
  if (r != Result_SUCCESS) {
    ERROR("verify_no_rollback: expecting success (3), got: %d", r);
    return -1;
  }

  r = no_rollback_check(verify_file, true, TEST_VERSION_MAGIC, NEXT_VERSION); // should succeed
  if (r != Result_SUCCESS) {
    ERROR("verify_no_rollback: expecting success (4), got: %d", r);
    return -1;
  }

  ERROR("(next line is expected to show red text...)");
  r = no_rollback_check(verify_file, true, TEST_VERSION_MAGIC, VERSION); // should fail with rollback detected
  if (r != Result_NO_ROLLBACK_INVALID_VERSION) {
    ERROR("verify_no_rollback: expecting incorrect version (2), got %d", r);
    return -1;
  }

  INFO("verify_no_rollback: ok");
  return 0;
}
