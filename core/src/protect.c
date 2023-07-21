#include "aes_gcm_common.h"

#define MAX_ENCRYPTED_PUBKEY_SIZE (XPUB_SIZE + AES_GCM_OVERHEAD_BYTES)
#define ENCRYPTED_MASTER_SEED_SIZE (MASTER_SEED_SIZE + AES_GCM_OVERHEAD_BYTES)

/**
 * Encrypt xpub with the pubkey encryption key.
 *
 * Zeros xpub.
 */
Result protect_pubkey(char xpub[static XPUB_SIZE],
                      EncryptedPubKey *encrypted_pub_key) {
  // Insert magic string for binary static analysis.
  // This is extremely hacky, but works. ¯\_(ツ)_/¯
#ifndef FUZZING_BUILD_MODE_UNSAFE_FOR_PRODUCTION
  printf(MAGIC);
#endif

  if (NULL == encrypted_pub_key) {
    ERROR("%s: null encrypted_pub_key input", __func__);
    return Result_UNKNOWN_INTERNAL_FAILURE;
  }

  static_assert(sizeof(encrypted_pub_key->encrypted_pub_key.bytes) >= MAX_ENCRYPTED_PUBKEY_SIZE,
                "misconfigured encrypted_pub_key max size");
  if (pub_key_encryption_key == 0) {
    ERROR("pub_key_encryption_key not initialized");
    return Result_PROTECT_PUBKEY_NO_PUBKEY_ENCRYPTION_KEY_FAILURE;
  }

  size_t ciphertext_len;

  pb_size_t xpub_len = (pb_size_t) strlen(xpub);

  Result r = aes_gcm_encrypt(pub_key_encryption_key,
                             (uint8_t *) xpub, xpub_len,
                             encrypted_pub_key->encrypted_pub_key.bytes,
                             sizeof(encrypted_pub_key->encrypted_pub_key.bytes),
                             &ciphertext_len);
  if (Result_SUCCESS != r) {
    ERROR("aes_gcm_encrypt failed (%d)", r);
    return r;
  }

  encrypted_pub_key->encrypted_pub_key.size = ciphertext_len;
  encrypted_pub_key->has_encrypted_pub_key = true;

  DEBUG("ciphertext_len: %zu", ciphertext_len);
  DEBUG_("ciphertext: ");
  for (unsigned int i = 0; i < ciphertext_len; i++) {
    DEBUG_("%02x", encrypted_pub_key->encrypted_pub_key.bytes[i]);
  }
  DEBUG_("\n");

  return Result_SUCCESS;
}

/**
 * Decrypt encrypted_pub_key with pubkey encryption key.
 */
Result expose_pubkey(const EncryptedPubKey* const encrypted_pub_key,
                     char xpub[static XPUB_SIZE]) {
  if (pub_key_encryption_key == 0) {
    ERROR("pub_key_encryption_key not initialized");
    return Result_EXPOSE_PUBKEY_NO_PUBKEY_ENCRYPTION_KEY_FAILURE;
  }

  if (NULL == encrypted_pub_key) {
    ERROR("%s: null encrypted_pub_key input", __func__);
    return Result_UNKNOWN_INTERNAL_FAILURE;
  }

  if (!encrypted_pub_key->has_encrypted_pub_key) {
    ERROR("missing encrypted_pub_key");
    return Result_EXPOSE_PUBKEY_NO_ENCRYPTED_PUBKEY_FAILURE;
  }
  if (encrypted_pub_key->encrypted_pub_key.size >= MAX_ENCRYPTED_PUBKEY_SIZE) {
    ERROR("unexpected encrypted_pub_key size");
    return Result_EXPOSE_PUBKEY_UNEXPECTED_ENCRYPTED_PUBKEY_SIZE_FAILURE;
  }

  size_t xpub_len;

  Result r = aes_gcm_decrypt(pub_key_encryption_key,
                             encrypted_pub_key->encrypted_pub_key.bytes,
                             encrypted_pub_key->encrypted_pub_key.size,
                             (uint8_t *)xpub, XPUB_SIZE - 1, &xpub_len);
  if (Result_SUCCESS != r) {
    ERROR("%s failed (%d)", __func__, r);
    return r;
  }

  // Null terminate xpub
  xpub[xpub_len] = 0;
  DEBUG("xpub: %s", xpub);

  return Result_SUCCESS;
}

/**
 * Encrypt master_seed with master_seed_encryption_key.
 *
 * Zeros master_seed.
 */
Result protect_wallet(uint8_t master_seed[static MASTER_SEED_SIZE],
                      EncryptedMasterSeed *encrypted_master_seed) {
  static_assert(sizeof(encrypted_master_seed->encrypted_master_seed.bytes) >=
                    MASTER_SEED_SIZE,
                "misconfigured encrypted_master_seed max size");

  if (master_seed_encryption_key == 0) {
    ERROR("master_seed_encryption_key not initialized");
    return Result_PROTECT_WALLET_NO_MASTER_SEED_ENCRYPTION_KEY_FAILURE;
  }

  size_t ciphertext_len;
  Result r = aes_gcm_encrypt(
      master_seed_encryption_key, master_seed, MASTER_SEED_SIZE,
      encrypted_master_seed->encrypted_master_seed.bytes,
      sizeof(encrypted_master_seed->encrypted_master_seed.bytes),
      &ciphertext_len);
  if (r != Result_SUCCESS) {
    ERROR("aes_gcm_encrypt failed (%d).", r);
    return r;
  }
  encrypted_master_seed->encrypted_master_seed.size = ciphertext_len;
  DEBUG("ciphertext_len: %zu", ciphertext_len);

  DEBUG_("ciphertext: ");
  for (unsigned int i = 0; i < ciphertext_len; i++) {
    DEBUG_("%02x", encrypted_master_seed->encrypted_master_seed.bytes[i]);
  }
  DEBUG_("\n");

  return Result_SUCCESS;
}

/**
 * Decrypt master_seed with master_seed_encryption_key.
 */
Result expose_wallet(const EncryptedMasterSeed* const encrypted_master_seed,
                     uint8_t master_seed[static MASTER_SEED_SIZE]) {
  if (master_seed_encryption_key == 0) {
    ERROR("master_seed_encryption_key not initialized");
    return Result_EXPOSE_WALLET_NO_MASTER_SEED_ENCRYPTION_KEY_FAILURE;
  }

  if (encrypted_master_seed->encrypted_master_seed.size != ENCRYPTED_MASTER_SEED_SIZE) {
    ERROR("Unexpected encrypted_master_seed size");
    return Result_EXPOSE_WALLET_UNEXPECTED_ENCRYPTED_MASTER_SEED_SIZE_FAILURE;
  }

  size_t master_seed_len;
  Result r = aes_gcm_decrypt(master_seed_encryption_key,
                             encrypted_master_seed->encrypted_master_seed.bytes,
                             encrypted_master_seed->encrypted_master_seed.size,
                             master_seed, MASTER_SEED_SIZE, &master_seed_len);
  if (r != Result_SUCCESS) {
    ERROR("aes_gcm_decrypt failed (%d).", r);
    return r;
  }
  if (master_seed_len != MASTER_SEED_SIZE) {
    ERROR("Unexpected master_seed_len.");
    return Result_EXPOSE_WALLET_UNEXPECTED_MASTER_SEED_LEN_FAILURE;
  }

  return Result_SUCCESS;
}
