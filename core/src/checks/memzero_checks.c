#include "checks.h"
#include "log.h"
#include "memzero.h"

#include <assert.h>
#include <string.h>

int verify_memzero(void) {
  uint8_t buf[16] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
  memzero(buf, sizeof(buf));
  uint8_t zero_buf[16] = { 0 };
  static_assert(sizeof(buf) == sizeof(zero_buf), "sizeof(buf) must equal sizeof(zero_buf)");
  if (memcmp(buf, zero_buf, sizeof(buf)) != 0) {
    ERROR("%s: memzero() failed to zero memory region", __func__);
    return -1;
  }
  INFO("%s: ok", __func__);
  return 0;
}
