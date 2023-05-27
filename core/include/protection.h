#pragma once

#include "config.h"
#include "squareup/subzero/common.pb.h"
#include "squareup/subzero/internal.pb.h"

#include <stdint.h>

/**
 * Encrypt xpub with the pubkey encryption key.
 *
 * Always zeros xpub.
 */
Result protect_pubkey(char xpub[static XPUB_SIZE],
                      EncryptedPubKey *encrypted_pub_key);

/**
 * Decrypt encrypted_pub_key with pubkey encryption key.
 */
Result expose_pubkey(const EncryptedPubKey* const encrypted_pub_key,
                     char xpub[static XPUB_SIZE]);

/**
 * Encrypt master_seed with master_seed_encryption_key.
 *
 * Zeros master_seed.
 */
Result protect_wallet(uint8_t master_seed[static MASTER_SEED_SIZE],
                      EncryptedMasterSeed *encrypted_master_seed);

/**
 * Decrypt master_seed with master_seed_encryption_key. In dev, we XOR with a
 * magic byte.
 */
Result expose_wallet(const EncryptedMasterSeed* const encrypted_master_seed,
                     uint8_t master_seed[static MASTER_SEED_SIZE]);
