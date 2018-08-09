#pragma once

#include <squareup/plutus/internal.pb.h>

Result aes_gcm_encrypt(M_KeyID keyId, uint8_t *plaintext, size_t plaintext_len,
                       uint8_t *ciphertext, size_t ciphertext_len,
                       size_t *bytes_written);
Result aes_gcm_decrypt(M_KeyID keyId, const uint8_t *ciphertext,
                       size_t ciphertext_len, uint8_t *plaintext,
                       size_t plaintext_len, size_t *bytes_written);
