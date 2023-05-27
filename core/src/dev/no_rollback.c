#include "no_rollback.h"

#include "config.h"
#include "log.h"

#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

Result no_rollback_read(const char* filename, char buf[static VERSION_SIZE]) {
  char tmp_file[100];
  snprintf(tmp_file, sizeof(tmp_file), "/tmp/%s", filename);

  FILE *f = fopen(tmp_file, "r");
  if (f == NULL) {
    // In dev, we magically create the file. It's one less thing to think about.
    no_rollback_write_version(filename, VERSION_MAGIC, VERSION);
    f = fopen(tmp_file, "r");
  }
  size_t bytes_read = fread(buf, 1, VERSION_SIZE, f);
  fclose(f);
  // Because the file is only supposed to be created by no_rollback_write,
  // any mismatch below indicates an anomaly. Hence we let it fail.
  if (bytes_read != VERSION_SIZE) {
    ERROR("%s failed", __func__);
    return Result_NO_ROLLBACK_FILE_NOT_FOUND;
  }
  if (buf[VERSION_SIZE - 1] != '\0') {
    ERROR("%s: rollback file contents not NULL-terminated", __func__);
    return Result_NO_ROLLBACK_INVALID_FORMAT;
  }
  return Result_SUCCESS;
}

Result no_rollback_write(const char* filename, char buf[static VERSION_SIZE]) {
  if (buf[VERSION_SIZE - 1] != '\0') {
    ERROR("%s: input buf is not NULL-terminated", __func__);
    return Result_NO_ROLLBACK_INVALID_FORMAT;
  }

  char tmp_file[100];
  snprintf(tmp_file, sizeof(tmp_file), "/tmp/%s", filename);

  FILE *f = fopen(tmp_file, "w");
  if (f == NULL) {
    ERROR("no_rollback_write failed");
    return Result_NO_ROLLBACK_FILE_NOT_FOUND;
  }
  size_t bytes_written = fwrite(buf, 1, VERSION_SIZE, f);
  fclose(f);
  if (bytes_written != VERSION_SIZE) {
    INFO("fwrite returned %zd, expecting %d", bytes_written, VERSION_SIZE);
    return Result_NO_ROLLBACK_FILE_NOT_FOUND;
  }

  return Result_SUCCESS;
}

