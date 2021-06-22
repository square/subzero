#pragma once

/* This header is meant for the nCipher target, and should only be included in the
 * codesafe/ncipher implementation */

// magic string for binary static analysis
// aes_gcm_ncipher:$(echo -n "aes_gcm_ncipher" | sha256sum | cut -c1-16)
#define MAGIC "aes_gcm_ncipher:ffb944993c73a1d3"

Result aes_gcm_encrypt(M_KeyID keyId, uint8_t *plaintext, size_t plaintext_len,
                       uint8_t *ciphertext, size_t ciphertext_len,
                       size_t *bytes_written);
Result aes_gcm_decrypt(M_KeyID keyId, const uint8_t *ciphertext,
                       size_t ciphertext_len, uint8_t *plaintext,
                       size_t plaintext_len, size_t *bytes_written);
