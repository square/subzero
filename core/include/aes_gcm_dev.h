#pragma once

#include "squareup/subzero/internal.pb.h"

#include <stddef.h>
#include <stdint.h>

/* This header is meant for the dev target, and should only be included in the
 * dev implementation */

// magic string for binary static analysis
// aes_gcm_dev:$(echo -n "aes_gcm_dev" | sha256sum | cut -c1-16)
#define MAGIC "aes_gcm_dev:8873b8689d31cc4d"

// M_KeyID typedef for the dev target
//
// Note that it's a re-definition of the codesafe-specific M_KeyID type. M_keyID
// is an integer typed handle to an HSM key object. The M_KeyID value 0 is
// reserved for an invalid key object in the nCipher target. We would like to
// simulate this behavior in the dev target, and therefore avoid value 0 for
// M_KeyID enum below.
typedef enum { master_seed_encryption_key = 1, pub_key_encryption_key = 2 } M_KeyID;

// Key encryption key declaration, for AES-128-GCM
// KEK[0], i.e., KEK[master_seed_encryption_key - 1], is master seed encryption key
// KEK[1], i.e., KEK[pub_key_encryption_key], is public key encryption key
extern uint8_t KEK[2][16];

Result aes_gcm_encrypt(
    M_KeyID keyId,
    uint8_t* plaintext,
    size_t plaintext_len,
    uint8_t* ciphertext,
    size_t ciphertext_len,
    size_t* bytes_written);
Result aes_gcm_decrypt(
    M_KeyID keyId,
    const uint8_t* ciphertext,
    size_t ciphertext_len,
    uint8_t* plaintext,
    size_t plaintext_len,
    size_t* bytes_written);
