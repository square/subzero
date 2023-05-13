#include "print.h"

#include "log.h"

#include <stdint.h>
#include <stdio.h>

// TODO: this file and hash.c have a lot of code in common. We should
// pull the common code in a set of serializer_* functions.

// print_uint prints hex in little-endian order (least-significant byte first)
static void print_uint(uint64_t value, uint8_t bytes) {
  for (int i = 0; i < bytes; i++) {
    // Some extra (uint8_t) casts here so clang-tidy is happy
    uint8_t byte = (uint8_t)(value & (uint8_t)0xFF);
    DEBUG_("%02x", byte);
    value = value >> (uint8_t)8;
  }
}

void print_uint8(const uint8_t value) { print_uint(value, 1); }

void print_uint16(const uint16_t value) { print_uint(value, 2); }

void print_uint32(const uint32_t value) { print_uint(value, 4); }

void print_uint64(const uint64_t value) { print_uint(value, 8); }

void print_bytes(const uint8_t *buffer, size_t len) {
  size_t i;
  for (i = 0; i < len; i++) {
    DEBUG_("%02x", buffer[i]);
  }
  DEBUG_("\n");
}

void print_rev_bytes(const uint8_t *buffer, size_t len) {
  size_t i;
  for (i = len; i-- > 0;) {
    DEBUG_("%02x", buffer[i]);
  }
  DEBUG_("\n");
}

void print_var_bytes(const uint8_t *buffer, size_t len) {
  if (len <= 252) {
    print_uint8(len);
  } else if (len <= 0xffff) {
    print_uint8(0xfd);
    print_uint16(len);
  } else if (len <= 0xffffffff) {
    print_uint8(0xfe);
    print_uint32(len);
  } else {
    print_uint8(0xff);
    print_uint64(len);
  }
  print_bytes(buffer, len);
}
