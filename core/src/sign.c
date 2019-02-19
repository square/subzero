#include <assert.h>
#include <base58.h>
#include <bip32.h>
#include <bip39.h>
#include <config.h>
#include <curves.h>
#include <print.h>
#include <protection.h>
#include <ripemd160.h>
#include <squareup/subzero/internal.pb.h>
#include <stdbool.h>
#include <stdio.h>

#include "bip32.h"
#include "conv.h"
#include "hash.h"
#include "log.h"
#include "rpc.h"
#include "script.h"
#include "sign.h"
#include "squareup/subzero/internal.pb.h"

static void compute_prevout_hash(TxInput *inputs, pb_size_t inputs_count,
                                 uint8_t hash[static HASHER_DIGEST_LENGTH]) {
  Hasher hasher;
  hasher_Init(&hasher, HASHER_SHA2);
  for (int i = 0; i < inputs_count; i++) {
    TxInput input = inputs[i];
    DEBUG("Computing prevout hash, input %d", i);
    hash_rev_bytes(&hasher, input.prev_hash, sizeof(input.prev_hash));
    print_rev_bytes(input.prev_hash, sizeof(input.prev_hash));

    hash_uint32(&hasher, input.prev_index);
    print_uint32(input.prev_index);
    DEBUG_("\n");
  }
  hasher_Double(&hasher, hash);
}

// This assumes all inputs have the same sequence value
static void compute_sequence_hash(uint32_t sequence, pb_size_t inputs_count,
                                  uint8_t hash[static HASHER_DIGEST_LENGTH]) {
  Hasher hasher;
  hasher_Init(&hasher, HASHER_SHA2);
  for (int i = 0; i < inputs_count; i++) {
    hash_uint32(&hasher, sequence);
  }
  hasher_Double(&hasher, hash);
}

/**
 * Takes an (decrypted) extended public key and produces the public key derived
 * from the path specified.
 */
static Result derive_public_key(const char *xpub, Path *path,
                                uint8_t public_key[static COMPRESSED_PUBKEY_SIZE]) {
  HDNode node;

  int r = hdnode_deserialize(xpub, PUBKEY_PREFIX, 0, SECP256K1_NAME, &node, NULL);
  if (r < 0) {
    return Result_DERIVE_PUBKEY_DESERIALIZE_FAILURE;
  }

  if (!path->has_is_change) {
    ERROR("path is missing is_change.");
    return Result_DERIVE_PUBKEY_NO_IS_CHANGE_FAILURE;
  }
  if (!path->has_index) {
    ERROR("path is missing index.");
    return Result_DERIVE_PUBKEY_NO_INDEX_FAILURE;
  }

  if (!hdnode_public_ckd(&node, path->is_change ? 1 : 0)) {
    ERROR("Error deriving public key path (for is_change)");
    return Result_DERIVE_PUBKEY_IS_CHANGE_FAILURE;
  }

  if (!hdnode_public_ckd(&node, path->index)) {
    ERROR("Error deriving public key path (for index)");
    return Result_DERIVE_PUBKEY_INDEX_FAILURE;
  }

  hdnode_fill_public_key(&node);
  memcpy(public_key, node.public_key, COMPRESSED_PUBKEY_SIZE);

  return Result_SUCCESS;
}

/**
 * Takes the seed and a path. Returns HDNode suitable for signing transactions
 * at that path.
 */
static Result derive_private_key(uint8_t seed[static SHA512_DIGEST_LENGTH],
                                 Path *path, HDNode *out) {
  if (hdnode_from_seed(seed, SHA512_DIGEST_LENGTH, SECP256K1_NAME, out) != 1) {
    ERROR("error: hdnode_from_seed failed.");
    return Result_DERIVE_PRIVATE_KEY_HDNODE_FROM_SEED_FAILURE;
  }

  if (!path->has_is_change) {
    ERROR("path is missing is_change.");
    return Result_DERIVE_PRIVATE_KEY_NO_IS_CHANGE_FAILURE;
  }

  // derive address m/coin'/change/index
  // where coin is 0' for BTC mainnet and 1' for BTC testnet.
  // See https://github.com/satoshilabs/slips/blob/master/slip-0044.md for the full list of coin types.
  if (!hdnode_private_ckd_prime(out, COIN_TYPE)) {
    ERROR("Error deriving private key path (for coin type)");
    return Result_DERIVE_PRIVATE_KEY_COIN_TYPE_FAILURE;
  }

  // This derivation should match the public key derivation above this.
  if (!hdnode_private_ckd(out, path->is_change ? 1 : 0)) {
    ERROR("Error deriving private key path (for is_change");
    return Result_DERIVE_PRIVATE_KEY_IS_CHANGE_FAILURE;
  }

  if (!hdnode_private_ckd(out, path->index)) {
    ERROR("Error deriving private key path (for index");
    return Result_DERIVE_PRIVATE_KEY_INDEX_FAILURE;
  }

  return Result_SUCCESS;
}

static void sort_public_keys(
    uint8_t unsorted_keys[static MULTISIG_PARTS][COMPRESSED_PUBKEY_SIZE],
    uint8_t sorted_keys[static MULTISIG_PARTS][COMPRESSED_PUBKEY_SIZE]) {
  // TODO: figure out how electrum (and other wallets) sort the keys. Do they
  // use the key before or after derivation?
  int i, j;
  for (i = 0; i < MULTISIG_PARTS; i++) {
    int score = 0;
    for (j = 0; j < MULTISIG_PARTS; j++) {
      if (strncmp((char *)unsorted_keys[i], (char *)unsorted_keys[j],
                  COMPRESSED_PUBKEY_SIZE) > 0) {
        score++;
      }
    }
    memcpy(sorted_keys[score], unsorted_keys[i], COMPRESSED_PUBKEY_SIZE);
  }
}

/**
 * Returns the script (i.e. 2 [addr1] [addr2] [addr3] [addr4] 4 checkmultisig).
 */
static Result multisig_script(script_t *script, char xpub[static MULTISIG_PARTS][XPUB_SIZE], Path *path) {
  // Derive addresses for this path
  uint8_t public_keys[MULTISIG_PARTS][COMPRESSED_PUBKEY_SIZE];
  for (int i = 0; i < MULTISIG_PARTS; i++) {
    Result r = derive_public_key(xpub[i], path, public_keys[i]);
    if (r != Result_SUCCESS) {
      ERROR("derive_public_key failed: (%d).", r);
      return r;
    }
  }

  // We have all the public keys. We need to sort them.
  uint8_t sorted_keys[MULTISIG_PARTS][COMPRESSED_PUBKEY_SIZE];
  sort_public_keys(public_keys, sorted_keys);

  // TODO: use MULTISIG_REQUIRED (need a way to convert to OP_2)
  static_assert(MULTISIG_REQUIRED == 2, "OP_2 is hardcoded here");
  Result r = script_push(script, OP_2);
  if (r != Result_SUCCESS) {
    ERROR("script_push failed: (%d).", r);
    return r;
  }

  for (int i = 0; i < MULTISIG_PARTS; i++) {
    r = script_push_data(script, sorted_keys[i], COMPRESSED_PUBKEY_SIZE);
    if (r != Result_SUCCESS) {
      ERROR("script_push_data failed: (%d).", r);
      return r;
    }
  }

  // TODO: use MULTISIG_PARTS (need a way to convert to OP_4)
  static_assert(MULTISIG_PARTS == 4, "OP_4 is hardcoded here");
  r = script_push(script, OP_4);
  if (r != Result_SUCCESS) {
    ERROR("script_push failed: (%d).", r);
    return r;
  }

  r = script_push(script, OP_CHECKMULTISIG);
  if (r != Result_SUCCESS) {
    ERROR("script_push failed: (%d).", r);
    return r;
  }

  return Result_SUCCESS;
}

/**
 * Verifies that the fee is less than 1 BTC or less than 10% of funds going to
 * gateway address The rules for fees are identical to those implemented in the
 * java module
 * @param request to validate fees for
 */
bool validate_fees(InternalCommandRequest_SignTxRequest *request) {
  // these are in satoshis
  uint64_t total = 0;
  uint64_t fee = 0;

  for (int i = 0; i < request->inputs_count; i++) {
    if (!request->inputs[i].has_amount) {
      ERROR("validate_fees: missing amount for input %d.", i);
      return false;
    }
    fee += request->inputs[i].amount;
  }
  for (int i = 0; i < request->outputs_count; i++) {
    if (!request->outputs[i].has_amount) {
      ERROR("validate_fees: missing amount for output %d.", i);
      return false;
    }

    if (fee < request->outputs[i].amount) {
      ERROR("validate_fees: fee underflow for output %d.", i);
      return false;
    }
    fee -= request->outputs[i].amount;

    if (request->outputs[i].destination == Destination_GATEWAY) {
      total += request->outputs[i].amount;
    }
  }

  return fee < conv_btc_to_satoshi(1) || fee * 10 < total;
}

static Result hash_input(char xpub[static MULTISIG_PARTS][XPUB_SIZE],
                         TxInput *input, uint32_t sequence, uint32_t lock_time,
                         uint8_t prevoutsHash[static HASHER_DIGEST_LENGTH],
                         uint8_t seqHash[static HASHER_DIGEST_LENGTH],
                         uint8_t outputHash[static HASHER_DIGEST_LENGTH],
                         uint8_t hash[static HASHER_DIGEST_LENGTH]) {

  Hasher hasher;

  DEBUG("hash_input");
  hasher_Init(&hasher, HASHER_SHA2);
  // 1. nVersion of the transaction (4-byte little endian)
  hash_uint32(&hasher, /*version*/ 1);
  print_uint32(1);

  // 2. hashPrevouts (32-byte hash)
  hash_bytes(&hasher, prevoutsHash, HASHER_DIGEST_LENGTH);
  print_bytes(prevoutsHash, HASHER_DIGEST_LENGTH);

  // 3. hashSequence (32-byte hash)
  hash_bytes(&hasher, seqHash, HASHER_DIGEST_LENGTH);
  print_bytes(seqHash, HASHER_DIGEST_LENGTH);

  // 4. outpoint (32-byte hash + 4-byte little endian)
  hash_rev_bytes(&hasher, input->prev_hash, HASHER_DIGEST_LENGTH);
  hash_uint32(&hasher, input->prev_index);
  print_rev_bytes(input->prev_hash, HASHER_DIGEST_LENGTH);
  print_uint32(input->prev_index);

  // 5. scriptCode of the input (serialized as scripts inside CTxOuts)
  script_t script = SCRIPT_EMPTY;
  Result r = multisig_script(&script, xpub, &input->path);
  if (r != Result_SUCCESS) {
    ERROR("multisig_script failed: (%d).", r);
    return r;
  }
  hash_var_bytes(&hasher, script.data, script.len);
  DEBUG("script.data:");
  print_var_bytes(script.data, script.len);

  // 6. value of the output spent by this input (8-byte little endian)
  hash_uint64(&hasher, input->amount);
  print_uint64(input->amount);

  // 7. nSequence of the input (4-byte little endian)
  hash_uint32(&hasher, sequence);
  print_uint32(sequence);

  // 8. hashOutputs (32-byte hash)
  hash_bytes(&hasher, outputHash, HASHER_DIGEST_LENGTH);
  print_bytes(outputHash, HASHER_DIGEST_LENGTH);

  // 9. nLocktime of the transaction (4-byte little endian)
  hash_uint32(&hasher, lock_time);
  print_uint32(lock_time);

  // 10. sighash type of the signature (4-byte little endian)
  hash_uint32(&hasher, 1); // hash type
  print_uint32(1);
  DEBUG_("\n");

  hasher_Double(&hasher, hash);
  DEBUG("That's it folks. Final input hash:");
  print_bytes(hash, HASHER_DIGEST_LENGTH);
  return Result_SUCCESS;
}

// hash_p2pkh_address derives a P2PKH address and hashes it into hasher
static Result hash_p2pkh_address(Hasher *hasher, const char *xpub, Path *path) {
  uint8_t public_key[COMPRESSED_PUBKEY_SIZE];
  Result r = derive_public_key(xpub, path, public_key);
  if (r != Result_SUCCESS) {
    ERROR("derive_public_key failed: (%d).", r);
    return r;
  }
  DEBUG("public_key: ");
  print_bytes(public_key, COMPRESSED_PUBKEY_SIZE);

  // Compute ripemd160(sha256(public_key)) for P2PKH
  uint8_t hash[HASHER_DIGEST_LENGTH];
  hasher_Raw(HASHER_SHA2, public_key, COMPRESSED_PUBKEY_SIZE, hash);
  ripemd160(hash, HASHER_DIGEST_LENGTH, hash);

  // Create a classic P2PKH script.
  script_t script = SCRIPT_EMPTY;
  r = script_push(&script, OP_DUP);
  if (r != Result_SUCCESS) {
    ERROR("script_push failed: (%d).", r);
    return r;
  }

  r = script_push(&script, OP_HASH160);
  if (r != Result_SUCCESS) {
    ERROR("script_push failed: (%d).", r);
    return r;
  }

  r = script_push_data(&script, hash, RIPEMD160_DIGEST_LENGTH);
  if (r != Result_SUCCESS) {
    ERROR("script_push_data failed: (%d).", r);
    return r;
  }

  r = script_push(&script, OP_EQUALVERIFY);
  if (r != Result_SUCCESS) {
    ERROR("script_push failed: (%d).", r);
    return r;
  }

  r = script_push(&script, OP_CHECKSIG);
  if (r != Result_SUCCESS) {
    ERROR("script_push failed: (%d).", r);
    return r;
  }

  hash_var_bytes(hasher, script.data, script.len);
  DEBUG("hashing output script:");
  print_var_bytes(script.data, script.len);
  return Result_SUCCESS;
}

// hash_change_address computes a change address for Path and hashes it.
// TODO: While this looks mostly right, it's untested.
static Result hash_change_address(Hasher *hasher,
                                  char xpub[static MULTISIG_PARTS][XPUB_SIZE],
                                  Path *path) {
  // Here we need to generate a P2SH-P2WSH change transaction back to the
  // cold wallet

  // First we construct the hash of a P2WSH program:
  // We get the hash of our usual multisig program

  /*
   * echo \
   * N \
   * [pubkey[0]] \
   * [pubkey[1]] \
   * ...
   * M CHECKMULTISIG \
   * | bx script-encode | bx sha256
   * SCRIPTHASH
   */

  script_t script = SCRIPT_EMPTY;
  Result r = multisig_script(&script, xpub, path);
  if (r != Result_SUCCESS) {
    ERROR("hash_script failed: (%d).", r);
    return r;
  }
  Hasher scripthasher;
  hasher_Init(&scripthasher, HASHER_SHA2);
  hash_bytes(&scripthasher, script.data, script.len);
  DEBUG("script.data:");
  print_bytes(script.data, script.len);

  uint8_t scripthash[HASHER_DIGEST_LENGTH];
  hasher_Final(&scripthasher, scripthash);

  DEBUG("scripthash: ");
  print_bytes(scripthash, HASHER_DIGEST_LENGTH);

  /* echo \
   * '0 [SCRIPTHASH]'\
   *  | bx script-encode | bx sha256 | bx ripemd160
   * P2WSH_SCRIPT_ASH
   */
  // then we turn it into a P2WSH program.
  script_t p2wsh = SCRIPT_EMPTY;
  r = script_push(&p2wsh, OP_0); // Segwit script version number
  if (r != Result_SUCCESS) {
    ERROR("script_push failed: (%d).", r);
    return r;
  }

  r = script_push_data(&p2wsh, scripthash, HASHER_DIGEST_LENGTH);
  if (r != Result_SUCCESS) {
    ERROR("script_push_data failed: (%d).", r);
    return r;
  }

  // Now compute ripemd160(sha256(p2wsh)) to put in the final P2SH
  uint8_t p2wsh_script_hash[HASHER_DIGEST_LENGTH];
  hasher_Raw(HASHER_SHA2, p2wsh.data, p2wsh.len, p2wsh_script_hash);

  uint8_t p2wsh_final_digest[RIPEMD160_DIGEST_LENGTH];
  ripemd160(p2wsh_script_hash, HASHER_DIGEST_LENGTH, p2wsh_final_digest);

  // finally, we construct the P2SH
  // HASH160 86762607e8fe87c0c37740cddee880988b9455b2 EQUAL
  script_t p2sh_script = SCRIPT_EMPTY;
  r = script_push(&p2sh_script, OP_HASH160);
  if (r != Result_SUCCESS) {
    ERROR("script_push failed: (%d).", r);
    return r;
  }

  r = script_push_data(&p2sh_script, p2wsh_final_digest, sizeof(p2wsh_final_digest));
  if (r != Result_SUCCESS) {
    ERROR("script_push_data failed: (%d).", r);
    return r;
  }

  r = script_push(&p2sh_script, OP_EQUAL);
  if (r != Result_SUCCESS) {
    ERROR("script_push failed: (%d).", r);
    return r;
  }

  hash_var_bytes(hasher, p2sh_script.data, p2sh_script.len);
  return Result_SUCCESS;
}

// compute_output_hash iterates over all
static Result
compute_output_hash(char xpub[static MULTISIG_PARTS][XPUB_SIZE],
                    TxOutput *outputs, pb_size_t outputs_count,
                    uint8_t output_hash[static HASHER_DIGEST_LENGTH]) {

  Hasher hasher;
  hasher_Init(&hasher, HASHER_SHA2);
  for (int i = 0; i < outputs_count; i++) {
    TxOutput out = outputs[i];

    hash_uint64(&hasher, out.amount);

    if (out.destination == Destination_CHANGE) {
      if (!out.path.is_change) {
        ERROR("Destination is change, but path isn't change");
        return Result_COMPUTE_OUTPUT_HASH_INVALID_DESTINATION_OR_PATH_FAILURE;
      }
      hash_change_address(&hasher, xpub, &out.path);
    } else {
      Result r = hash_p2pkh_address(&hasher, GATEWAY, &out.path);
      if (r != Result_SUCCESS) {
        ERROR("hash_p2pkh_address failed: (%d).", r);
        return r;
      }
    }
  }
  hasher_Double(&hasher, output_hash);
  return Result_SUCCESS;
}

/**
 * Perform a transaction signing operation. We use deterministic signatures
 * (rfc6979) which makes checking the validity of the operation easier.
 *
 * The code creates two signatures for a 2-of-4 segwit wallet. The result can't
 * be verified with Electrum but we can broadcast it on the Testnet.
 */
Result handle_sign_tx(InternalCommandRequest_SignTxRequest *request,
                      InternalCommandResponse_SignTxResponse *response) {

  if (!validate_fees(request)) {
    ERROR("validate_fees failed");
    return Result_FEE_VALIDATION_FAILED;
  }

  // Load encrypted wallet
  uint8_t seed[SHA512_DIGEST_LENGTH];
  Result r = expose_wallet(&request->encrypted_master_seed, seed);
  if (r != Result_SUCCESS) {
    ERROR("expose_wallet failed: (%d).", r);
    return r;
  }

  // Load enc_pub_keys
  char xpub[MULTISIG_PARTS][XPUB_SIZE];
  for (int i = 0; i < MULTISIG_PARTS; i++) {
    r = expose_pubkey(&request->encrypted_pub_keys[i], xpub[i]);
    if (r != Result_SUCCESS) {
      ERROR("expose_pubkey failed");
      return r;
    }
    DEBUG("Loaded pubkey %d: %s", i, xpub[i]);
  }

  DEBUG("Gateway: %s", GATEWAY);

  // Compute what BIP 0143 calls "midstate": three hashes shared by all
  // signatures below.
  uint8_t prevoutsHash[HASHER_DIGEST_LENGTH];
  compute_prevout_hash(request->inputs, request->inputs_count, prevoutsHash);
  DEBUG("prevoutsHash");
  print_bytes(prevoutsHash, HASHER_DIGEST_LENGTH);

  uint8_t seqHash[HASHER_DIGEST_LENGTH];
  // We assume all inputs have this fixed sequence. TODO: Do they? If not, they
  // need to be in the request.
  uint32_t sequence = 0xfffffffe;
  compute_sequence_hash(sequence, request->inputs_count, seqHash);
  DEBUG("seqHash");
  print_bytes(seqHash, HASHER_DIGEST_LENGTH);

  uint8_t outputHash[HASHER_DIGEST_LENGTH];
  r = compute_output_hash(xpub, request->outputs, request->outputs_count, outputHash);
  if (r != Result_SUCCESS) {
    ERROR("compute_output_hash failed: (%d).", r);
    return r;
  }
  DEBUG("outputHash");
  print_bytes(outputHash, HASHER_DIGEST_LENGTH);

  // Create a signature for each input.
  for (int i = 0; i < request->inputs_count; i++) {

    // Compute hash to sign: BIP-0143
    uint8_t hash[HASHER_DIGEST_LENGTH];

    r = hash_input(xpub, &request->inputs[i], sequence, request->lock_time,
                   prevoutsHash, seqHash, outputHash, hash);
    if (r != Result_SUCCESS) {
      ERROR("hash_input failed: (%d).", r);
      return r;
    }

    // Derive the private key used for this input
    HDNode wallet;
    r = derive_private_key(seed, &request->inputs[i].path, &wallet);
    if (r != Result_SUCCESS) {
      ERROR("derive_private_key failed: (%d).", r);
      return r;
    }
    hdnode_fill_public_key(&wallet);
    // Validate the pubkey we're signing with is one of our public keys.
    bool found = false;
    for (int j = 0; j < MULTISIG_PARTS; j++) {
      uint8_t public_key[33];
      r = derive_public_key(xpub[j], &request->inputs[i].path, public_key);
      if (r != Result_SUCCESS) {
        ERROR("Failed deriving public key");
        return r;
      }
      static_assert(sizeof(public_key) == sizeof(wallet.public_key),
                    "Pubkey size mismatch");
      if (memcmp(public_key, wallet.public_key, sizeof(public_key)) == 0) {
        DEBUG("Signing for pubkey %d", j);
        found = true;
      }
    }
    if (!found) {
      ERROR("We're signing with a private key that doesn't match one of our "
            "public keys");
      print_bytes(wallet.public_key, sizeof(wallet.public_key));
      return Result_UNKNOWN_INTERNAL_FAILURE;
    }

    // sign the hash
    uint8_t sig[64];
    if (hdnode_sign_digest(&wallet, hash, sig, NULL, NULL) != 0) {
      ERROR("hdnode_sign_digest failed");
      return Result_UNKNOWN_INTERNAL_FAILURE;
    }

    // Validate the signature.  Validating after signing is important to ensure
    // we are operating correctly, and can also potentially prevent some types
    // of glitch attacks.
    hdnode_fill_public_key(&wallet);
    if (ecdsa_verify_digest(wallet.curve->params, wallet.public_key, sig,
                            hash) != 0) {
      ERROR("Verifying signature we just created failed");
      return Result_UNKNOWN_INTERNAL_FAILURE;
    } else {
      DEBUG("Successfully validated with public key:");
      print_bytes(wallet.public_key, 33);
    }

    int der_len = ecdsa_sig_to_der(sig, response->signatures[i].der.bytes);
    response->signatures[i].has_der = true;
    response->signatures[i].der.size = (pb_size_t)der_len;
    DEBUG("Signature:");
    for (int j = 0; j < der_len; j++) {
      DEBUG_("%02x", response->signatures[i].der.bytes[j]);
    }
    DEBUG_("\n");

    response->signatures[i].has_hash = true;
    static_assert(sizeof(hash) == sizeof(response->signatures[i].hash),
                  "Expect hash size to match proto size");
    memcpy(response->signatures[i].hash, hash, sizeof(hash));
  }
  response->signatures_count = request->inputs_count;

  return Result_SUCCESS;
}
