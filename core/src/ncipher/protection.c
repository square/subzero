#include <assert.h>
#include <nfastapp.h>
#include <protection.h>
#include <seelib.h>
#include <squareup/plutus/common.pb.h>
#include <squareup/plutus/internal.pb.h>

#include "aes_gcm.h"
#include "config.h"
#include "log.h"

M_KeyID master_seed_encryption_key = 0;
M_KeyID pub_key_encryption_key = 0;

/**
 * Encrypt xpub with the pubkey encryption key.
 *
 * Zeros xpub.
 */
Result protect_pubkey(char xpub[static XPUB_SIZE],
                      EncryptedPubKey *encrypted_pub_key) {
  static_assert(sizeof(encrypted_pub_key->encrypted_pub_key.bytes) >=
                    XPUB_SIZE + 16 + 12,
                "misconfigured encrypted_pub_key max size");
  if (pub_key_encryption_key == 0) {
    ERROR("pub_key_encryption_key not initialized");
    return Result_PROTECT_PUBKEY_NO_PUBKEY_ENCRYPTION_KEY_FAILURE;
  }

  size_t ciphertext_len;
  Result r = aes_gcm_encrypt(
      pub_key_encryption_key, (uint8_t *)xpub, strlen(xpub),
      encrypted_pub_key->encrypted_pub_key.bytes,
      sizeof(encrypted_pub_key->encrypted_pub_key.bytes), &ciphertext_len);
  if (r != Result_SUCCESS) {
    ERROR("aes_gcm_encrypt failed (%d)", r);
    return r;
  }
  encrypted_pub_key->encrypted_pub_key.size = ciphertext_len;
  encrypted_pub_key->has_encrypted_pub_key = true;
  DEBUG("ciphertext_len: %d", ciphertext_len);

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
Result expose_pubkey(EncryptedPubKey *encrypted_pub_key,
                     char xpub[static XPUB_SIZE]) {
  if (pub_key_encryption_key == 0) {
    ERROR("pub_key_encryption_key not initialized");
    return Result_EXPOSE_PUBKEY_NO_PUBKEY_ENCRYPTION_KEY_FAILURE;
  }
  if (!encrypted_pub_key->has_encrypted_pub_key) {
    ERROR("missing encrypted_pub_key");
    return Result_EXPOSE_PUBKEY_NO_ENCRYPTED_PUBKEY_FAILURE;
  }
  if (encrypted_pub_key->encrypted_pub_key.size >= (XPUB_SIZE + 16 + 12)) {
    ERROR("unexpected encrypted_pub_key size");
    return Result_EXPOSE_PUBKEY_UNEXPECTED_ENCRYPTED_PUBKEY_SIZE_FAILURE;
  }

  size_t xpub_len;
  Result r = aes_gcm_decrypt(pub_key_encryption_key,
                             encrypted_pub_key->encrypted_pub_key.bytes,
                             encrypted_pub_key->encrypted_pub_key.size,
                             (uint8_t *)xpub, XPUB_SIZE - 1, &xpub_len);
  if (r != Result_SUCCESS) {
    ERROR("aes_gcm_decrypt failed (%d).", r);
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
  DEBUG("ciphertext_len: %d", ciphertext_len);

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
Result expose_wallet(EncryptedMasterSeed *encrypted_master_seed,
                     uint8_t master_seed[static MASTER_SEED_SIZE]) {
  if (master_seed_encryption_key == 0) {
    ERROR("master_seed_encryption_key not initialized");
    return Result_EXPOSE_WALLET_NO_MASTER_SEED_ENCRYPTION_KEY_FAILURE;
  }

  if (encrypted_master_seed->encrypted_master_seed.size !=
      MASTER_SEED_SIZE + 16 + 12) {
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

  printf("master_seed: ");
  for (unsigned int i = 0; i < r; i++) {
    DEBUG_("%02x", master_seed[i]);
  }
  DEBUG_("\n");

  return Result_SUCCESS;
}
