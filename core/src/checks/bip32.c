#include <stdio.h>
#include <string.h>

#include "bip32.h"
#include "bip39.h"
#include "curves.h"

#include "config.h"
#include "checks.h"
#include "log.h"
#include "squareup/plutus/internal.pb.h"

/**
 * Perform BIP32 derivation using a hardcoded mnemonic and verify that we get
 * specific private/public keys back.
 *
 * I used https://iancoleman.io/bip39/ to generate these.
 *
 * TODO(alok): use "official" test vectors?
 */
int verify_bip32(void) {
  const char *mnemonic = "license amount assist beach story farm duck among "
                         "door meat prepare path";
  if (!mnemonic_check(mnemonic)) {
    ERROR("mnemonic_check failed.");
    return -1;
  }

  // Derive and check seed
  uint8_t seed[SHA512_DIGEST_LENGTH];
  mnemonic_to_seed(mnemonic, /* passphrase */ "", seed,
                   /* progress_callback */ NULL);
  uint8_t expected_seed[SHA512_DIGEST_LENGTH] = {
      0x16, 0x0a, 0x87, 0x8c, 0x27, 0xef, 0xfd, 0xb5, 0x6f, 0xff, 0xda,
      0x39, 0x11, 0xed, 0x1f, 0x2f, 0x04, 0x80, 0x44, 0x2b, 0xed, 0x6b,
      0xd1, 0x81, 0xb1, 0x23, 0xbe, 0xa7, 0x26, 0x80, 0x4e, 0x33, 0xac,
      0x38, 0x4b, 0xf2, 0x8a, 0xfc, 0x25, 0x83, 0xbf, 0x97, 0x2a, 0x1a,
      0x41, 0x9b, 0x6c, 0x89, 0x7f, 0xae, 0x6c, 0x8a, 0x48, 0x23, 0x76,
      0x27, 0xbe, 0x86, 0xa5, 0xaa, 0xde, 0x06, 0x70, 0xb3};
  if (memcmp(seed, expected_seed, SHA512_DIGEST_LENGTH) != 0) {
    ERROR("seed != expected_seed");
    return -1;
  }

  HDNode node;
  int r = hdnode_from_seed(seed, sizeof(seed), SECP256K1_NAME, &node);
  if (r != 1) {
    ERROR("hdnode_from_seed failed (%d).", r);
    return -1;
  }

  hdnode_fill_public_key(&node);
  char str[112];

  // Check root node
  r = hdnode_serialize_private(&node, 0, PRIVKEY_PREFIX, str, sizeof(str));
  if (r <= 0) {
    ERROR("hdnode_serialize_private failed (%d).", r);
    return -1;
  }
  ASSERT_STR_EQUAL(str, BIP32_TEST_ROOT_PRIVKEY, "unexpected root private key.");

  r = hdnode_serialize_public(&node, 0, PUBKEY_PREFIX, str, sizeof(str));
  if (r <= 0) {
    ERROR("hdnode_serialize_public failed (%d).", r);
    return -1;
  }
  ASSERT_STR_EQUAL(str, BIP32_TEST_ROOT_PUBKEY, "unexpected root public key.");

  // 123rd address
  // m/44'/coin/0'/0/123
  hdnode_private_ckd_prime(&node, 44);
  hdnode_private_ckd_prime(&node, COIN_TYPE);
  hdnode_private_ckd_prime(&node, 0);
  hdnode_private_ckd(&node, 0);
  uint32_t fingerprint = hdnode_fingerprint(&node);
  hdnode_private_ckd(&node, 123);
  hdnode_fill_public_key(&node);
  hdnode_serialize_private(&node, fingerprint, PRIVKEY_PREFIX, str, sizeof(str));
  if (r <= 0) {
    ERROR("hdnode_serialize_private failed (%d).", r);
    return -1;
  }
  ASSERT_STR_EQUAL(str, BIP32_TEST_CHILD_PRIVKEY, "unexpected child private key.");

  hdnode_serialize_public(&node, fingerprint, PUBKEY_PREFIX, str, sizeof(str));
  if (r <= 0) {
    ERROR("hdnode_serialize_public failed (%d).", r);
    return -1;
  }
  ASSERT_STR_EQUAL(str, BIP32_TEST_CHILD_PUBKEY, "unexpected child public key.");

  INFO("verify_bip32 ok");

  return 0;
}
