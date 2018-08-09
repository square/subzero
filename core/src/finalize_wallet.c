#include <assert.h>
#include <bip32.h>
#include <curves.h>
#include <squareup/plutus/internal.pb.h>

#include "config.h"
#include "log.h"
#include "protection.h"
#include "rpc.h"

Result
handle_finalize_wallet(InternalCommandRequest_FinalizeWalletRequest *in,
                       InternalCommandResponse_FinalizeWalletResponse *out) {
  if (in->encrypted_pub_keys_count != MULTISIG_PARTS) {
    ERROR("expecting %d encrypted_pub_keys, received %d.", MULTISIG_PARTS,
          in->encrypted_pub_keys_count);
    return Result_MISSING_ARGUMENTS;
  }

  // Decrypt encrypted_master_seed
  uint8_t master_seed[MASTER_SEED_SIZE];
  Result r = expose_wallet(&in->encrypted_master_seed, master_seed);
  if (r != Result_SUCCESS) {
    ERROR("expose_wallet failed: (%d).", r);
    return r;
  }

  // Decrypt each encrypted_pub_keys
  char pub_keys[MULTISIG_PARTS][XPUB_SIZE];
  for (int i = 0; i < MULTISIG_PARTS; i++) {
    r = expose_pubkey(&in->encrypted_pub_keys[i], pub_keys[i]);
    if (r != Result_SUCCESS) {
      ERROR("expose_pubkey failed: (%d).", r);
      return r;
    }
  }

  // TODO: use authenticated headers to ensure all public keys are tied to the
  // same wallet_id.
  // TODO: ensure public keys originate from different HSMs.
  // TODO: ensure public keys are associated to the same currency.

  // check that all public keys are different
  for (int i = 0; i < MULTISIG_PARTS; i++) {
    for (int j = 0; j < MULTISIG_PARTS; j++) {
      if (i == j) {
        continue;
      }
      if (strncmp(pub_keys[i], pub_keys[j], XPUB_SIZE) == 0) {
        ERROR("pub_key %d is the same as pub_key %d.", i, j);
        return Result_UNKNOWN_INTERNAL_FAILURE;
      }
    }
  }

  // check that one of the public keys matches our master_seed
  bool found = false;
  int i;
  for (i = 0; i < MULTISIG_PARTS; i++) {
    HDNode node;
    // TODO: error handling!
    hdnode_from_seed(master_seed, sizeof(master_seed), SECP256K1_NAME, &node);

    // We have to perform the first derivation (0' for Mainnet, 1' for Testnet) before getting
    // the pubkey
    // TODO: error handling!
    uint32_t fingerprint = hdnode_fingerprint(&node);
    hdnode_private_ckd_prime(&node, COIN_TYPE);
    hdnode_fill_public_key(&node);

    char pub_key[XPUB_SIZE];
    int ret = hdnode_serialize_public(&node, fingerprint, PUBKEY_PREFIX,
                                      pub_key, sizeof(pub_key));
    if (ret <= 0) {
      ERROR("hdnode_serialize_public failed");
      return Result_UNKNOWN_INTERNAL_FAILURE;
    }
    if (strcmp(pub_keys[i], pub_key) == 0) {
      found = true;
      break;
    }
  }
  if (!found) {
    ERROR("master_seed does not match encrypted_pub_keys");
    return Result_UNKNOWN_INTERNAL_FAILURE;
  }

  static_assert(sizeof(out->pub_key.bytes) == XPUB_SIZE,
                "misconfigured pub_key max size");
  strcpy((char *)out->pub_key.bytes, pub_keys[i]);
  out->pub_key.size = (pb_size_t)strlen(pub_keys[i]);

  // TODO: figure out pub_keys_hash story
  out->pub_keys_hash.bytes[0] = 0x99;
  out->pub_keys_hash.size = 1;

  return Result_SUCCESS;
}
