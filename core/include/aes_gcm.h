#pragma once

#include <squareup/subzero/internal.pb.h>

// M_KeyID is typedef'ed in codesafe header. If STDMARSHALTYPES_H is not
// defined, we are in dev target
#ifndef STDMARSHALTYPES_H
typedef enum {KEYID_MASTER_SEED_ENCRYPTION_KEY = 0,
              KEYID_PUBKEY_ENCRYPTION_KEY = 1} M_KeyID;
// Key encryption key declaration (AES-256-GCM)
extern uint8_t KEK[2][32];
#endif


Result aes_gcm_encrypt(M_KeyID keyId, uint8_t *plaintext, size_t plaintext_len,
                       uint8_t *ciphertext, size_t ciphertext_len,
                       size_t *bytes_written);
Result aes_gcm_decrypt(M_KeyID keyId, const uint8_t *ciphertext,
                       size_t ciphertext_len, uint8_t *plaintext,
                       size_t plaintext_len, size_t *bytes_written);
