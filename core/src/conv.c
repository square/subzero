#include "conv.h"

#include "log.h"

#include <assert.h>

uint64_t conv_btc_to_satoshi(uint32_t btc) {
  return (uint64_t) btc * 100000000ull;
}

bool u16_to_little_endian_bytes(const uint16_t value, uint8_t* const buf, const size_t buf_len) {
  static_assert(sizeof(uint16_t) == 2, "sizeof(uint16_t) != 2");
  if (sizeof(uint16_t) != buf_len) {
    ERROR("%s: buf_len == %zu, expected: %zu", __func__, buf_len, sizeof(uint16_t));
    return false;
  }
  if (NULL == buf) {
    ERROR("%s: input buf is NULL", __func__);
    return false;
  }
  buf[0] = (uint8_t) (value & 0xFF);
  buf[1] = (uint8_t) ((value >> 8) & 0xFF);
  return true;
}

bool u32_to_little_endian_bytes(const uint32_t value, uint8_t* const buf, const size_t buf_len) {
  static_assert(sizeof(uint32_t) == 4, "sizeof(uint32_t) != 4");
  if (sizeof(uint32_t) != buf_len) {
    ERROR("%s: buf_len == %zu, expected: %zu", __func__, buf_len, sizeof(uint32_t));
    return false;
  }
  if (NULL == buf) {
    ERROR("%s: input buf is NULL", __func__);
    return false;
  }
  buf[0] = (uint8_t) (value & 0xFF);
  buf[1] = (uint8_t) ((value >> 8) & 0xFF);
  buf[2] = (uint8_t) ((value >> 16) & 0xFF);
  buf[3] = (uint8_t) ((value >> 24) & 0xFF);
  return true;
}
