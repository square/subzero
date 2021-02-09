#include <stdint.h>
#include "log.h"
#include "memzero.h"
#include "aes_gcm.h"
#include "gcm.h"
#include "rand.h"

// Temp buffer for backing up content, etc
uint8_t aes_gcm_buffer[1000];

/**
 * AES-GCM encryption. Used to encrypt the wallet. Also used to encrypt the
 * public key. When this function returns, plaintext is zeroed out.
 *
 * Implementation detail:
 * Ciphertext is [IV (12 bytes), ciphertext (same length as input), TAG (16
 * bytes)].
 */
Result aes_gcm_encrypt(M_KeyID keyId, uint8_t * plaintext, size_t plaintext_len,
                       uint8_t *ciphertext, size_t ciphertext_len,
                       size_t *bytes_written)
{
  uint8_t iv[12] = {0};
  uint8_t tag[16] = {0};
  gcm_ctx ctx[1] = {0};

  memzero(ciphertext, ciphertext_len);

  size_t expected_ciphertext_len = plaintext_len + sizeof(iv) + sizeof(tag);
  if (ciphertext_len < expected_ciphertext_len) {
      ERROR("aes_gcm_encrypt: ciphertext buffer too small.");
      memzero(plaintext, plaintext_len);
      return Result_AES_GCM_ENCRYPT_BUFFER_TOO_SMALL_FAILURE;
  }
  if (expected_ciphertext_len > sizeof(aes_gcm_buffer)) {
      ERROR("aes_gcm_encrypt: plaintext too long.");
      memzero(plaintext, plaintext_len);
      return Result_AES_GCM_ENCRYPT_PLAINTEXT_TOO_LONG_FAILURE;
  }

  if (RETURN_GOOD != gcm_init_and_key(KEK[keyId], sizeof(KEK[keyId]), ctx))
  {
      ERROR("gcm_init_and_key failed");
      return Result_UNKNOWN_INTERNAL_FAILURE;
  }

  // Generate a random IV
  random_buffer(iv, sizeof(iv));

  // Encrypt plaintext
  memcpy(aes_gcm_buffer, plaintext, plaintext_len);

  if (RETURN_GOOD != gcm_encrypt_message(iv, sizeof(iv),
                                         NULL, 0, // empty header
                                         aes_gcm_buffer, plaintext_len,
                                         tag, sizeof(tag),
                                         ctx))
  {
    ERROR("gcm_encrypt_message failed");
    memzero(aes_gcm_buffer, sizeof(aes_gcm_buffer));
    return Result_UNKNOWN_INTERNAL_FAILURE;
  }

  if (RETURN_GOOD != gcm_end(ctx))
  {
    ERROR("gcm_end failed");
    return Result_UNKNOWN_INTERNAL_FAILURE;
  }

  memcpy(ciphertext, iv, sizeof(iv));
  memcpy(ciphertext + sizeof(iv), aes_gcm_buffer, plaintext_len);
  memcpy(ciphertext + sizeof(iv) + plaintext_len, tag, sizeof(tag));
  *bytes_written = expected_ciphertext_len;
  memzero(aes_gcm_buffer, sizeof(aes_gcm_buffer));

  return Result_SUCCESS;
}

/**
 * AES-GCM decryption.
 */
Result aes_gcm_decrypt(M_KeyID keyId, const uint8_t *ciphertext, size_t ciphertext_len,
                       uint8_t *plaintext, size_t plaintext_len,
                       size_t *bytes_written)
{
  uint8_t iv[12] = {0};
  uint8_t tag[16] = {0};
  gcm_ctx ctx[1] = {0};

  memzero(plaintext, plaintext_len);

  if (ciphertext_len < sizeof(iv) + sizeof(tag))
  {
    ERROR("%s: ciphertext buffer too small.", __func__);
    return Result_AES_GCM_DECRYPT_BUFFER_TOO_SMALL_FAILURE;
  }

  if (ciphertext_len > sizeof(aes_gcm_buffer)) {
    ERROR("aes_gcm_decrypt: ciphertext too long.");
    return Result_AES_GCM_DECRYPT_CIPHERTEXT_TOO_LONG_FAILURE;
  }

  size_t expected_plaintext_len = ciphertext_len - sizeof(iv) - sizeof(tag);
  if (plaintext_len < expected_plaintext_len) {
    ERROR("aes_gcm_decrypt: plaintext buffer too small.");
    return Result_AES_GCM_DECRYPT_BUFFER_TOO_SMALL_FAILURE;
  }

  if (RETURN_GOOD != gcm_init_and_key(KEK[keyId], sizeof(KEK[keyId]), ctx))
  {
    ERROR("gcm_init_and_key failed");
    return Result_UNKNOWN_INTERNAL_FAILURE;
  }

  // Decrypt ciphertext
  memcpy(iv, ciphertext, sizeof(iv));
  memcpy(aes_gcm_buffer, ciphertext + sizeof(iv), expected_plaintext_len);
  memcpy(tag, ciphertext + sizeof(iv) + expected_plaintext_len, sizeof(tag));

  if (RETURN_GOOD != gcm_decrypt_message(iv, sizeof(iv),
                                         NULL, 0, // empty header
                                         aes_gcm_buffer, expected_plaintext_len,
                                         tag, sizeof(tag),
                                         ctx))
  {
    ERROR("gcm_decrypt_message failed");
    memzero(aes_gcm_buffer, sizeof(aes_gcm_buffer));
    memzero(iv, sizeof(iv));
    memzero(tag, sizeof(tag));
    return Result_UNKNOWN_INTERNAL_FAILURE;
  }

  if (RETURN_GOOD != gcm_end(ctx))
  {
    ERROR("gcm_end failed");
    return Result_UNKNOWN_INTERNAL_FAILURE;
  }

  memcpy(plaintext, aes_gcm_buffer, expected_plaintext_len);
  *bytes_written = expected_plaintext_len;
  memzero(aes_gcm_buffer, sizeof(aes_gcm_buffer));
  memzero(iv, sizeof(iv));
  memzero(tag, sizeof(tag));

  return Result_SUCCESS;
}

