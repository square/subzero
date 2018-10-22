#include "base58.h"
#include "bip32.h"
#include "bip39.h"
#include "curves.h"
#include <assert.h>
#include <config.h>
#include <protection.h>
#include <rpc.h>
#include <squareup/subzero/common.pb.h>
#include <squareup/subzero/internal.pb.h>
#include <stdio.h>
#include <string.h>

#include <assert.h>

#include "checks.h"
#include "conv.h"
#include "hash.h"
#include "log.h"
#include "print.h"
#include "script.h"
#include "sign.h"
#include "squareup/subzero/internal.pb.h"

// Return a constructed request for test
// See test-transaction.txt for info on the data used here
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
  tx->inputs[0].path.has_account = true;
  tx->inputs[0].path.account = 0;
  tx->inputs[0].path.has_is_change = true;
  tx->inputs[0].path.is_change = false;
  tx->inputs[0].path.has_index = true;
  tx->inputs[0].path.index = 0;

  tx->outputs_count = 1;
  tx->outputs[0].has_amount = true;
  tx->outputs[0].amount = 999334;

  tx->outputs[0].has_path = true;
  tx->outputs[0].path.has_account = true;
  tx->outputs[0].path.account = 0;
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
    ERROR("unexpected der_len: %d != %ld.", resp.signatures[0].der.size,
          sizeof(expected_der));
    return -1;
  }
  if (memcmp(resp.signatures[0].der.bytes, expected_der,
             resp.signatures[0].der.size) != 0) {
    ERROR("der mismatch.");
    for (int i=0; i<resp.signatures[0].der.size; i++) {
      printf("0x%02x, ", resp.signatures[0].der.bytes[i]);
    }
    return -1;
  }
  INFO("sign_tx passed");
  return 0;
}
