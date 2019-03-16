#pragma once

#include <squareup/subzero/internal.pb.h>

#include "config.h"

Result no_rollback(void);
Result no_rollback_check(const char* filename, bool allow_upgrade, uint32_t expected_magic, uint32_t expected_version);
Result no_rollback_write_version(const char* filename, uint32_t magic, uint32_t version);

Result no_rollback_read(const char* filename, char buf[static VERSION_SIZE]);
Result no_rollback_write(const char* filename, char buf[static VERSION_SIZE]);
