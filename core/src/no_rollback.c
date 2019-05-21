#include "no_rollback.h"
#include "log.h"

/**
 * Prevents running an older version of core after a newer version has been seen. The goal is to limit the attack
 * surface to only the current version (vs any version which ever existed).
  *
 * The code in this file is dev/ncipher agnostic. We prevent rollback by persisting a magic number and a version number.
 * The magic number prevents accidentally mixing rollback protection code from another application. The version is
 * a high water mark and gets automatically bumped if needed when the application starts.
 *
 * dev/no_rollback.c uses a file (/tmp/subzero00).
 * ncipher/no_rollback.c uses the NVRAM, which needs to be configured with the proper ACLs when the HSM is enrolled.
 */
Result no_rollback(void) {
  INFO("in no_rollback");

  return no_rollback_check("subzero00", true, VERSION_MAGIC, VERSION);
}

Result no_rollback_check(const char* filename, bool allow_upgrade, uint32_t expected_magic, uint32_t expected_version) {
  char buf[VERSION_SIZE];

  Result r = no_rollback_read(filename, buf);
  if (r != Result_SUCCESS) {
    ERROR("no_rollback_check: no_rollback_read failed");
    return r;
  }

  uint32_t magic, version, matches;
  matches = sscanf(buf, "%u-%u", &magic, &version);
  if (matches != 2) {
    ERROR("no_rollback_check: invalid format failure");
    return Result_NO_ROLLBACK_INVALID_FORMAT;
  }
  if (magic != expected_magic) {
    ERROR("no_rollback_check: invalid magic failure");
    return Result_NO_ROLLBACK_INVALID_MAGIC;
  }

  if (allow_upgrade && (version < expected_version)) {
    INFO("no_rollback_check: bumping version from %d to %d", version, expected_version);
    r = no_rollback_write_version(filename, expected_magic, expected_version);
    if (r != Result_SUCCESS) {
      return r;
    }

    // re-read to ensure everything is ok
    return no_rollback_check(filename, false, expected_magic, expected_version);
  } else if (version == expected_version) {
    INFO("no_rollback: ok");
    return Result_SUCCESS;
  }
  ERROR("no_rollback_check: invalid version failure");
  return Result_NO_ROLLBACK_INVALID_VERSION;
}

Result no_rollback_write_version(const char* filename, uint32_t magic, uint32_t version) {
  DEBUG("in no_rollback_write");

  char buf[VERSION_SIZE];
  bzero(buf, VERSION_SIZE);
  snprintf(buf, sizeof(buf), "%d-%d", magic, version);
  return no_rollback_write(filename, buf);
}
