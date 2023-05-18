#include "checks.h"
#include "conv.h"
#include "log.h"

#include <inttypes.h>
#include <stdint.h>

int verify_conv_btc_to_satoshi(void) {
  uint32_t btc = 0;
  uint64_t satoshi = conv_btc_to_satoshi(btc);
  uint64_t expected = 0;
  if (satoshi != expected) {
    ERROR("%s: conv_btc_to_satoshi(%" PRIu32 ") returned %" PRIu64 " (expected: %" PRIu64 ")",
          __func__,
          btc,
          satoshi,
          expected);
    return -1;
  }

  btc = 1;
  satoshi = conv_btc_to_satoshi(btc);
  expected = 100000000ull;
  if (satoshi != expected) {
    ERROR("%s: conv_btc_to_satoshi(%" PRIu32 ") returned %" PRIu64 " (expected: %" PRIu64 ")",
          __func__,
          btc,
          satoshi,
          expected);
    return -1;
  }

  btc = 21000000; // ~21 million BTC is the most that will ever be mined
  satoshi = conv_btc_to_satoshi(btc);
  expected = 2100000000000000ull;
  if (satoshi != expected) {
    ERROR("%s: conv_btc_to_satoshi(%" PRIu32 ") returned %" PRIu64 " (expected: %" PRIu64 ")",
          __func__,
          btc,
          satoshi,
          expected);
    return -1;
  }

  INFO("verify_conv_btc_to_satoshi: ok");
  return 0;
}

int verify_u16_to_little_endian_bytes(void) {
  uint8_t buf[sizeof(uint16_t)] = { 0 };
  const uint16_t value = (uint16_t) 0x0102;

  // positive test
  if (!u16_to_little_endian_bytes(value, buf, sizeof(buf))) {
    ERROR("%s: u16_to_little_endian_bytes() failed", __func__);
    return -1;
  }
  if (buf[0] != 2 || buf[1] != 1) {
    ERROR("%s: u16_to_little_endian_bytes() wrote incorrect buf: 0x%hhx%hhx", __func__, buf[0], buf[1]);
    return -1;
  }

  // negative tests
  ERROR("(next line is expected to show red text...)");
  if (u16_to_little_endian_bytes(value, buf, sizeof(buf) - 1)) {
    ERROR("%s: u16_to_little_endian_bytes() should have rejected wrong-size buf but didn't", __func__);
    return -1;
  }
  ERROR("(next line is expected to show red text...)");
  if (u16_to_little_endian_bytes(value, NULL, sizeof(buf))) {
    ERROR("%s: u16_to_little_endian_bytes() should have rejected NULL buf but didn't", __func__);
    return -1;
  }

  INFO("%s: ok", __func__);
  return 0;
}

int verify_u32_to_little_endian_bytes(void) {
  uint8_t buf[sizeof(uint32_t)] = { 0 };
  const uint32_t value = (uint32_t) 0x01020304;

  // positive test
  if (!u32_to_little_endian_bytes(value, buf, sizeof(buf))) {
    ERROR("%s: u32_to_little_endian_bytes() failed", __func__);
    return -1;
  }
  if (buf[0] != 4 || buf[1] != 3 || buf[2] != 2 || buf[3] != 1) {
    ERROR(
        "%s: u32_to_little_endian_bytes() wrote incorrect buf: 0x%hhx%hhx%hhx%hhx",
        __func__,
        buf[0],
        buf[1],
        buf[2],
        buf[3]);
    return -1;
  }

  // negative tests
  ERROR("(next line is expected to show red text...)");
  if (u32_to_little_endian_bytes(value, buf, sizeof(buf) - 1)) {
    ERROR("%s: u32_to_little_endian_bytes() should have rejected wrong-size buf but didn't", __func__);
    return -1;
  }
  ERROR("(next line is expected to show red text...)");
  if (u32_to_little_endian_bytes(value, NULL, sizeof(buf))) {
    ERROR("%s: u32_to_little_endian_bytes() should have rejected NULL buf but didn't", __func__);
    return -1;
  }

  INFO("%s: ok", __func__);
  return 0;
}
