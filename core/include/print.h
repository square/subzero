#pragma once

#include <stddef.h>
#include <stdint.h>

void print_uint8(uint8_t value);
void print_uint16(uint16_t value);
void print_uint32(uint32_t value);
void print_uint64(uint64_t value);
void print_bytes(const uint8_t* buffer, size_t len);
void print_rev_bytes(const uint8_t* buffer, size_t len);
void print_var_bytes(const uint8_t* buffer, size_t len);
