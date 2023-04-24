#include "checks.h"
#include "log.h"

#include <sha2.h>
#include <stdint.h>

int verify_byte_order(void) {
  union {
    uint8_t bytes[sizeof(uint32_t) / sizeof(uint8_t)];
    uint32_t word;
  } u;
  u.word = 1;
  const bool is_little_endian = (u.bytes[0] == (uint8_t) 1);
#if BYTE_ORDER == LITTLE_ENDIAN
  if (!is_little_endian) {
    ERROR("%s: detected byte order is big-endian, but -DBYTE_ORDER=4321 is not defined", __func__);
    return -1;
  }
#else // if BYTE_ORDER == BIG_ENDIAN
  if (is_little_endian) {
    ERROR("%s: detected byte order is little-endian, but -DBYTE_ORDER=4321 is defined", __func__);
    return -1;
  }
#endif
  INFO("%s: ok", __func__);
  return 0;
}
