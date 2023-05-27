#include "hash.h"

#include "hasher.h"

// hash_uint hashes in little-endian order (least-significant byte first)
static void hash_uint(Hasher *hasher, uint64_t value, uint8_t bytes) {
  for (int i = 0; i < bytes; i++) {
    // Some extra (uint8_t) casts here so clang-tidy is happy
    uint8_t byte = (uint8_t)(value & (uint8_t)0xFF);
    hasher_Update(hasher, &byte, 1);
    value = value >> (uint8_t)8;
  }
}

void hash_uint8(Hasher *hasher, uint8_t value) { hash_uint(hasher, value, 1); }

void hash_uint16(Hasher *hasher, uint16_t value) {
  hash_uint(hasher, value, 2);
}

void hash_uint32(Hasher *hasher, uint32_t value) {
  hash_uint(hasher, value, 4);
}

void hash_uint64(Hasher *hasher, uint64_t value) {
  hash_uint(hasher, value, 8);
}

void hash_bytes(Hasher *hasher, const uint8_t *data, size_t len) {
  hasher_Update(hasher, data, len);
}

void hash_rev_bytes(Hasher *hasher, const uint8_t *data, size_t len) {
  for (size_t i = len; i-- > 0;) {
    hasher_Update(hasher, &data[i], 1);
  }
}

void hash_var_bytes(Hasher *hasher, const uint8_t *data, size_t len) {
  if (len <= 252) {
    hash_uint8(hasher, len);
  } else if (len <= 0xffff) {
    hash_uint8(hasher, 0xfd);
    hash_uint16(hasher, len);
  } else if (len <= 0xffffffff) {
    hash_uint8(hasher, 0xfe);
    hash_uint32(hasher, len);
  } else {
    hash_uint8(hasher, 0xff);
    hash_uint64(hasher, len);
  }
  hash_bytes(hasher, data, len);
}
