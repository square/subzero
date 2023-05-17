#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#define QRSIGNATURE_LEN ((size_t) 64)
#define QRSIGNATURE_PUBKEY_LEN ((size_t) 65)

/**
 * signature verification wrapper for trezor crypto util.
 * Input should be raw bytes(unhashed). This function will hash input with sha256
 * before verification.
 *
 * Preconditions:
 * - None of the pointers may be NULL.
 * - data_len must be > 0.
 * - signature_len must equal QRSIGNATURE_LEN.
 * 
 * @return true on successful signature verification and false otherwise.
 */
bool check_qrsignature(
    const uint8_t* const data,
    const size_t data_len,
    const uint8_t* const signature,
    const size_t signature_len);

/**
 * Same as above.
 * You get to choose the public key to verify sig with.
 * So it can be used in self checks.
 *
 * Preconditions:
 * - None of the pointers may be NULL.
 * - data_len must be > 0.
 * - signature_len must equal QRSIGNATURE_LEN.
 * - pubkey_len must equal QRSIGNATURE_PUBKEY_LEN
 */
bool check_qrsignature_pub(
    const uint8_t* const data,
    const size_t data_len,
    const uint8_t* const signature,
    const size_t signature_len,
    const uint8_t* const pubkey,
    const size_t pubkey_len);
