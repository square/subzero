#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#include "no_rollback.h"
#include "config.h"
#include "log.h"

static void no_rollback_write(void);

/**
 * In dev, no_rollback simulates prod by using a file instead. We however don't fail if the file does not exist (and
 * instead automatically create it):
 *
 * - if file doesn't exist:
 *   - write the current version to file.
 * - else:
 *   - read the version as an int.
 *   - if the version is older than the current version:
 *     - write the current version to file.
 *   - else if the version is newer than the current version:
 *     - exit(-1).
 */
void no_rollback() {
  FILE *f = fopen(NO_ROLLBACK_DEV_FILE, "r");
  if (f == NULL) {
    // create the file
    no_rollback_write();
    return;
  }
  // read & compare version
  uint32_t magic, version;
  DEBUG("reading version from %s", NO_ROLLBACK_DEV_FILE);
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
    no_rollback_write();
  } else {
    assert(version == VERSION);
    INFO("version match.");
  }
}

static void no_rollback_write() {
  // create the file
  FILE *f = fopen(NO_ROLLBACK_DEV_FILE, "w");
  if (f == NULL) {
    ERROR("Failed to write %s. Exiting.", NO_ROLLBACK_DEV_FILE);
    exit(-1);
  }
  // write version
  DEBUG("writing version file (%s).", NO_ROLLBACK_DEV_FILE);
  fprintf(f, "%d-%d", VERSION_MAGIC, VERSION);
  fclose(f);
}
