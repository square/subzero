#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#include "check_ver.h"
#include "config.h"
#include "log.h"

static void check_ver_write(void);

/**
 * In dev, check_ver simulates prod by uses a file instead.
 *
 * - if nvram/file doesn't exist:
 *   - write the current version to nvram/file.
 * - else:
 *   - read the version as an int.
 *   - if the version is older than the current version:
 *     - write the current version to nvram/file.
 *   - else if the version is newer than the current version:
 *     - exit(-1).
 */
void check_ver() {
  FILE *f = fopen(VERSION_FILE, "r");
  if (f == NULL) {
    // create the file
    check_ver_write();
    return;
  }
  // read & compare version
  uint32_t magic, version;
  DEBUG("reading version from %s", VERSION_FILE);
  fscanf(f, "%u-%u", &magic, &version);
  fclose(f);
  if (magic != VERSION_MAGIC) {
    ERROR("Unexpected magic number. Exiting");
    exit(-1);
  }
  if (version > VERSION) {
    ERROR("Rollback detected! Exiting");
    exit(-1);
  } else if (version < VERSION) {
    check_ver_write();
  } else {
    assert(version == VERSION);
    INFO("version match.");
  }
}

static void check_ver_write() {
  // create the file
  FILE *f = fopen(VERSION_FILE, "w");
  if (f == NULL) {
    ERROR("Failed to write %s. Exiting.", VERSION_FILE);
    exit(-1);
  }
  // write version
  DEBUG("writing version file (%s).", VERSION_FILE);
  fprintf(f, "%d-%d", VERSION_MAGIC, VERSION);
  fclose(f);
}
