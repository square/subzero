#include "conv.h"

uint64_t conv_btc_to_satoshi(uint32_t btc) {
  return (uint64_t) btc * 100000000ull;
}
