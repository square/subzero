#include <assert.h>
#include <protection.h>
#include <squareup/plutus/common.pb.h>
#include <squareup/plutus/internal.pb.h>

#include "log.h"
#include "memzero.h"

// Protection: developer edition
// We do janky fake crypto in dev, to make it easy on us.
// XOR with a fixed byte!
static uint8_t MAGIC = 0xAA;
static uint8_t MOREMAGIC = 0x55;

/**
 * Encrypt xpub. In dev, we XOR with a magic byte.
 *
 * Always zeros xpub and succeeds.
 */
Result protect_pubkey(char xpub[static XPUB_SIZE],
                      EncryptedPubKey *encrypted_pub_key) {
  static_assert(sizeof(encrypted_pub_key->encrypted_pub_key.bytes) >= XPUB_SIZE,
                "misconfigured encrypted_pub_key max size");
  pb_size_t len = (pb_size_t)strlen(xpub);

  for (int i = 0; i < len; i++) {
    encrypted_pub_key->encrypted_pub_key.bytes[i] = (uint8_t)xpub[i] ^ MAGIC;
  }
  encrypted_pub_key->encrypted_pub_key.size = len;
  encrypted_pub_key->has_encrypted_pub_key = true;
  memzero(xpub, XPUB_SIZE);
  return Result_SUCCESS;
}

/**
 * Decrypt encrypted_pub_key. In dev, we XOR with a magic byte.
 */
Result expose_pubkey(EncryptedPubKey *encrypted_pub_key,
                     char xpub[static XPUB_SIZE]) {
  if (!encrypted_pub_key->has_encrypted_pub_key) {
    ERROR("missing encrypted_pub_key");
    return Result_EXPOSE_PUBKEY_NO_ENCRYPTED_PUBKEY_FAILURE;
  }
  if (encrypted_pub_key->encrypted_pub_key.size >= XPUB_SIZE) {
    ERROR("unexpected encrypted_pub_key size");
    return Result_EXPOSE_PUBKEY_UNEXPECTED_ENCRYPTED_PUBKEY_SIZE_FAILURE;
  }

  for (int i = 0; i < encrypted_pub_key->encrypted_pub_key.size; i++) {
    xpub[i] = encrypted_pub_key->encrypted_pub_key.bytes[i] ^ MAGIC;
  }
  xpub[encrypted_pub_key->encrypted_pub_key.size] = 0x00;
  return Result_SUCCESS;
}

/**
 * Encrypt master_seed. In dev, we XOR with a magic byte.
 *
 * Always Zeros master_seed and succeeds.
 */
Result protect_wallet(uint8_t master_seed[static MASTER_SEED_SIZE],
                      EncryptedMasterSeed *encrypted_master_seed) {
  static_assert(sizeof(encrypted_master_seed->encrypted_master_seed.bytes) >=
                    MASTER_SEED_SIZE,
                "misconfigured encrypted_master_seed max size");

  // XOR with MOREMAGIC
  for (int i = 0; i < MASTER_SEED_SIZE; i++) {
    encrypted_master_seed->encrypted_master_seed.bytes[i] =
        master_seed[i] ^ MOREMAGIC;
  }
  encrypted_master_seed->encrypted_master_seed.size = MASTER_SEED_SIZE;
  memzero(master_seed, MASTER_SEED_SIZE);
  return Result_SUCCESS;
}

/**
 * Decrypt master_seed. In dev, we XOR with a magic byte.
 */
Result expose_wallet(EncryptedMasterSeed *encrypted_master_seed,
                     uint8_t master_seed[static MASTER_SEED_SIZE]) {
  if (encrypted_master_seed->encrypted_master_seed.size != MASTER_SEED_SIZE) {
    ERROR("Unexpected encrypted_master_seed size");
    return Result_EXPOSE_WALLET_UNEXPECTED_ENCRYPTED_MASTER_SEED_SIZE_FAILURE;
  }

  // XOR with MOREMAGIC
  for (int i = 0; i < encrypted_master_seed->encrypted_master_seed.size; i++) {
    master_seed[i] =
        encrypted_master_seed->encrypted_master_seed.bytes[i] ^ MOREMAGIC;
  }
  return Result_SUCCESS;
}
