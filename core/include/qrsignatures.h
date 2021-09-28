#pragma once


/**
 * signature verification wrapper for trezor crypto util.
 * Input should be raw bytes(unhashed). This function will hash input with sha256
 * before verification.
 * 
 * @return true on successful signature verification and false otherwise.
 */

bool check_qrsignature(const uint8_t * const data, size_t data_len, const uint8_t * const signature);

