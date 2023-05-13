#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

/**
 * signature verification wrapper for trezor crypto util.
 * Input should be raw bytes(unhashed). This function will hash input with sha256
 * before verification.
 * 
 * @return true on successful signature verification and false otherwise.
 */
bool check_qrsignature(const uint8_t * const data, size_t data_len, const uint8_t * const signature);

/**
 * Same as above.
 * You get to choose the public key to verify sig with.
 * So it can be used in self checks.
 */
bool check_qrsignature_pub(const uint8_t * const data, size_t data_len, const uint8_t * const signature, const uint8_t * const pubkey);


