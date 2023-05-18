#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

/**
 * Converts the given amount in BTC to the equivalent amount in Satoshis.
 * This just multiplies by 100 million, since 1 BTC == 100,000,000 Satoshis.
 */
uint64_t conv_btc_to_satoshi(uint32_t btc);

/**
 * Writes the given 16-bit unsigned integer to the given buffer in little endian byte order.
 * Preconditions:
 * - buf_len must equal sizeof(uint16_t)
 * - buf must not be NULL.
 *
 * Returns true on success or false if any of the preconditions were not met.
 */
bool u16_to_little_endian_bytes(const uint16_t value, uint8_t* const buf, const size_t buf_len);

/**
 * Writes the given 32-bit unsigned integer to the given buffer in little endian byte order.
 * Preconditions:
 * - buf_len must equal sizeof(uint32_t)
 * - buf must not be NULL.
 *
 * Returns true on success or false if any of the preconditions were not met.
 */
bool u32_to_little_endian_bytes(const uint32_t value, uint8_t* const buf, const size_t buf_len);
