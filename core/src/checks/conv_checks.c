#include "checks.h"
#include "conv.h"
#include "log.h"

#include <inttypes.h>

int verify_conv_btc_to_satoshi(void) {
  uint32_t btc = 0;
  uint64_t satoshi = conv_btc_to_satoshi(btc);
  uint64_t expected = 0;
  if (satoshi != expected) {
    ERROR(
        "%s: conv_btc_to_satoshi(%" PRIu32 ") returned %" PRIu64 " (expected: %" PRIu64 ")",
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
    ERROR(
        "%s: conv_btc_to_satoshi(%" PRIu32 ") returned %" PRIu64 " (expected: %" PRIu64 ")",
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
    ERROR(
        "%s: conv_btc_to_satoshi(%" PRIu32 ") returned %" PRIu64 " (expected: %" PRIu64 ")",
        __func__,
        btc,
        satoshi,
        expected);
    return -1;
  }

  return 0;
}
