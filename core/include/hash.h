#pragma once

#include <hasher.h>
#include <stddef.h>
#include <stdint.h>

void hash_uint8(Hasher* hasher, uint8_t value);
void hash_uint16(Hasher* hasher, uint16_t value);
void hash_uint32(Hasher* hasher, uint32_t value);
void hash_uint64(Hasher* hasher, uint64_t value);
// hashes are serialized in "normal" order
void hash_bytes(Hasher* hasher, const uint8_t* data, size_t len);
// addresses are serialized in reverse order
void hash_rev_bytes(Hasher* hasher, const uint8_t* data, size_t len);
// scripts are serialized with a length prefix
void hash_var_bytes(Hasher* hasher, const uint8_t* data, size_t len);
