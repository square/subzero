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

/**
 * This hard-coded test case verifies that no_rollback_write_to_buf() produces a known output from the current
 * VERSION_MAGIC and VERSION constants. It needs to be updated when the VERSION is incremented.
 */
int verify_no_rollback_write_to_buf(void) {
  char buf[VERSION_SIZE];
  char expected_buf[VERSION_SIZE] = { 0 };
  static_assert(sizeof(buf) == sizeof(expected_buf), "buf and expected_buf should have the same size");

  no_rollback_write_to_buf(VERSION_MAGIC, VERSION, buf);

  // Note: the VERSION string is at the end. Update it when updating the VERSION constant.
  const char* expected_string = "8414-210";
  memcpy(expected_buf, expected_string, strlen(expected_string));

  int res = memcmp(buf, expected_buf, sizeof(buf));
  if (0 != res) {
    ERROR("%s: buffers were not equal: buf == %s, expected_buf == %s", __func__, buf, expected_buf);
  }
  INFO("%s: ok", __func__);
  return res;
}
