#pragma once

#include <squareup/subzero/internal.pb.h>

// M_KeyID is typedef'ed in codesafe header. If STDMARSHALTYPES_H is not
// defined, we are in dev target
#ifndef STDMARSHALTYPES_H
typedef enum {master_seed_encryption_key = 1,
              pub_key_encryption_key = 2} M_KeyID;
// Key encryption key declaration, for AES-128-GCM
// KEK[0], i.e., KEK[master_seed_encryption_key - 1], is master seed encryption key
// KEK[1], i.e., KEK[pub_key_encryption_key], is public key encryption key
extern uint8_t KEK[2][16];
#endif


Result aes_gcm_encrypt(M_KeyID keyId, uint8_t *plaintext, size_t plaintext_len,
                       uint8_t *ciphertext, size_t ciphertext_len,
                       size_t *bytes_written);
Result aes_gcm_decrypt(M_KeyID keyId, const uint8_t *ciphertext,
                       size_t ciphertext_len, uint8_t *plaintext,
                       size_t plaintext_len, size_t *bytes_written);
