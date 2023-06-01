#include "bip39.h"
#include "checks.h"
#include "curves.h"
#include "ecdsa.h"
#include "hasher.h"
#include "log.h"
#include "nist256p1.h"
#include "pb.h"
#include "print.h"
#include "qrsignatures.h"
#include "sign.h"
#include "squareup/subzero/common.pb.h"
#include "squareup/subzero/internal.pb.h"

#include <assert.h>
#include <config.h>
#include <protection.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

// Return a constructed request for test
static int construct_request(InternalCommandRequest_SignTxRequest *tx) {

  const char *mnemonic1 =
      "turn inch relief grit abuse machine riot proof they model way dad "
      "pelican oven gold spoil cave gloom dismiss dress leader scale isolate "
      "tribe";
  if (!mnemonic_check(mnemonic1)) {
    ERROR("mnemonic_check failed.");
    return -1;
  }
  uint8_t seed[MASTER_SEED_SIZE];
  static_assert(MASTER_SEED_SIZE >= 64, "MASTER_SEED_SIZE too small");
  mnemonic_to_seed(mnemonic1, /* passphrase */ "", seed,
                   /* progress_callback */ NULL);

  // "Encrypt" seed as if it had come from init_wallet
  Result r = protect_wallet(seed, &tx->encrypted_master_seed);
  if (r != Result_SUCCESS) {
    ERROR("protect_wallet failed: (%d).", r);
    return -1;
  }

  char wallets[MULTISIG_PARTS][XPUB_SIZE] = {TEST_WALLET_1, TEST_WALLET_2, TEST_WALLET_3, TEST_WALLET_4};

  // Encrypt each pubkey as if it had come from finalize_wallet
  for (int i = 0; i < MULTISIG_PARTS; i++) {
    r = protect_pubkey(wallets[i], &tx->encrypted_pub_keys[i]);
    if (r != Result_SUCCESS) {
      ERROR("protect_pubkey failed: (%d).", r);
      return -1;
    }
  }
  tx->encrypted_pub_keys_count = 4;

  // Create signature
  tx->inputs_count = 1;
  pb_byte_t prev_hash[] = {0xd3, 0x73, 0x65, 0xc6, 0xa3, 0xc0, 0x89, 0x57,
                           0x6b, 0x1e, 0xa1, 0xcb, 0x8b, 0xee, 0x2b, 0xaa,
                           0x00, 0x5a, 0xfc, 0x96, 0xd2, 0x40, 0x80, 0x51,
                           0x56, 0x35, 0xf8, 0x52, 0xc6, 0x80, 0xd1, 0x27};
  memcpy(tx->inputs[0].prev_hash, prev_hash, sizeof(prev_hash));
  tx->inputs[0].prev_index = 0;
  tx->inputs[0].has_amount = true;
  tx->inputs[0].amount = 1000000;

  tx->inputs[0].has_path = true;
  tx->inputs[0].path.has_is_change = true;
  tx->inputs[0].path.is_change = false;
  tx->inputs[0].path.has_index = true;
  tx->inputs[0].path.index = 0;

  tx->outputs_count = 1;
  tx->outputs[0].has_amount = true;
  tx->outputs[0].amount = 999334;

  tx->outputs[0].has_path = true;
  tx->outputs[0].path.has_is_change = true;
  tx->outputs[0].path.is_change = false;
  tx->outputs[0].path.has_index = true;
  tx->outputs[0].path.index = 0;

  tx->outputs[0].destination = Destination_GATEWAY;

  tx->lock_time = 0;
  return 0;
}

/**
 *  Self-check with a fixed transaction.
 */
int verify_sign_tx(void) {
  InternalCommandRequest_SignTxRequest tx =
      InternalCommandRequest_SignTxRequest_init_default;
  construct_request(&tx);

  InternalCommandResponse_SignTxResponse resp =
      InternalCommandResponse_SignTxResponse_init_default;
  INFO("checking sign_tx.");
  Result r = handle_sign_tx(&tx, &resp);
  if (r != Result_SUCCESS) {
    ERROR("handle_sign_tx failed: (%d).", r);
    return -1;
  }

  uint8_t expected_der[] = EXPECTED_SIGNATURE;

  if (resp.signatures_count != tx.inputs_count) {
    ERROR("Unexpected signatures_count: %d != %d", resp.signatures_count,
          tx.inputs_count);
    return -1;
  }
  if (resp.signatures[0].der.size != sizeof(expected_der)) {
    ERROR("unexpected der_len: %d != %zu.", resp.signatures[0].der.size,
          sizeof(expected_der));
    return -1;
  }
  if (memcmp(resp.signatures[0].der.bytes, expected_der,
             resp.signatures[0].der.size) != 0) {
    ERROR("der mismatch.");
    print_bytes(expected_der, sizeof(expected_der));
    print_bytes(resp.signatures[0].der.bytes, resp.signatures[0].der.size);
    return -1;
  }
  
  return 0;
}

/**
 * This function does not make an effort to zeroize
 * anything as all of the values are test values.
 * It will just test our verify signature wrapper.
 */

int verify_check_qrsignature_pub(void){
  INFO("Checking check_qrsignature_pub.");
  // Test ECDSA private key.
  uint8_t private_key[32] = {
    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01
  };
  // bytes to be signed.
  uint8_t buf[8] = {
    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01
  };

  uint8_t signature[QRSIGNATURE_LEN] = {0};

  // echo "ef3fd8719c72b1410df9494c61bc99825b73a2236b54832d524dc9f8d261c3c04c7b5c0f1527a74e3432f319e576fba8ac402b378c47fed2760dfc933e382096" | xxd -r -p | xxd -i 
  uint8_t expected_signature[QRSIGNATURE_LEN] = {
    0xef, 0x3f, 0xd8, 0x71, 0x9c, 0x72, 0xb1, 0x41, 0x0d, 0xf9, 0x49, 0x4c,
    0x61, 0xbc, 0x99, 0x82, 0x5b, 0x73, 0xa2, 0x23, 0x6b, 0x54, 0x83, 0x2d,
    0x52, 0x4d, 0xc9, 0xf8, 0xd2, 0x61, 0xc3, 0xc0, 0x4c, 0x7b, 0x5c, 0x0f,
    0x15, 0x27, 0xa7, 0x4e, 0x34, 0x32, 0xf3, 0x19, 0xe5, 0x76, 0xfb, 0xa8,
    0xac, 0x40, 0x2b, 0x37, 0x8c, 0x47, 0xfe, 0xd2, 0x76, 0x0d, 0xfc, 0x93,
    0x3e, 0x38, 0x20, 0x96
  };


  if(0 != ecdsa_sign(
      (const ecdsa_curve *)&nist256p1,
      HASHER_SHA2,
      private_key,
      buf,
      sizeof(buf),
      signature,
      NULL,
      NULL
    )
  ){
    ERROR("Could not sign during self check.");
    return 1;
  }
  DEBUG("Signature:");
  print_bytes(signature, QRSIGNATURE_LEN);

  if(memcmp(signature, expected_signature, sizeof(signature)) != 0){
    ERROR("Expected signature was not generated.");
    return 2;
  }

  uint8_t pub[QRSIGNATURE_PUBKEY_LEN] = {0};
  ecdsa_get_public_key65((const ecdsa_curve *)&nist256p1, private_key, pub);
  // flip sig bits.
  signature[sizeof(signature) - 1] ^= 0xff;
  // This should return false.
  if (check_qrsignature_pub(buf, sizeof(buf), signature, sizeof(signature), pub, sizeof(pub))) {
    ERROR("Signature should not have verified.");
    return 3;
  }
  signature[sizeof(signature) - 1] ^= 0xff;

  // flip data bits.
  buf[0] ^= 0xff;
  //this should return false.
  if (check_qrsignature_pub(buf, sizeof(buf), signature, sizeof(signature), pub, sizeof(pub))) {
    ERROR("Signature should not have verified with wrong data.");
    return 4;
  }
  buf[0] ^= 0xff;

  // flip public key bits.
  pub[sizeof(pub) / 2] ^= 0xff;
  if (check_qrsignature_pub(buf, sizeof(buf), signature, sizeof(signature), pub, sizeof(pub))) {
    ERROR("Signature should not have verified with wrong public key.");
    return 5;
  }
  pub[sizeof(pub) / 2] ^= 0xff;

  // Attempting to verify a null data should return false
  ERROR("(next line is expected to show red text...)");
  if (check_qrsignature_pub(NULL, sizeof(buf), signature, sizeof(signature), pub, sizeof(pub))) {
    ERROR("Signature should not have verified with NULL data.");
    return 6;
  }

  // Attempting to verify a zero-length data should return false
  ERROR("(next line is expected to show red text...)");
  if (check_qrsignature_pub(buf, 0, signature, sizeof(signature), pub, sizeof(pub))) {
    ERROR("Signature should not have verified with zero-length data.");
    return 7;
  }

  // Attempting to verify a NULL signature should return false
  ERROR("(next line is expected to show red text...)");
  if (check_qrsignature_pub(buf, sizeof(buf), NULL, sizeof(signature), pub, sizeof(pub))) {
    ERROR("Signature should not have verified with NULL signature.");
    return 8;
  }

  // Attempting to verify a wrong-length signature should return false
  ERROR("(next line is expected to show red text...)");
  if (check_qrsignature_pub(buf, sizeof(buf), signature, sizeof(signature) - 1, pub, sizeof(pub))) {
    ERROR("Signature should not have verified with a wrong-length signature.");
    return 9;
  }

  // Attempting to verify with a NULL pub key should return false
  ERROR("(next line is expected to show red text...)");
  if (check_qrsignature_pub(buf, sizeof(buf), signature, sizeof(signature), NULL, sizeof(pub))) {
    ERROR("Signature should not have verified with NULL pub key.");
    return 10;
  }

  // Attempting to verify with a wrong-length pub key should return false
  ERROR("(next line is expected to show red text...)");
  if (check_qrsignature_pub(buf, sizeof(buf), signature, sizeof(signature), pub, sizeof(pub) - 1)) {
    ERROR("Signature should not have verified with a wrong-length pub key.");
    return 11;
  }

  // Positive test: verifying an unmodified signature should work.
  if (!check_qrsignature_pub(buf, sizeof(buf), signature, sizeof(signature), pub, sizeof(pub))) {
    ERROR("verify signature does not seem to work.");
    return 12;
  }

  return 0;
}
