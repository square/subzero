#include "checks.h"
#include "config.h"
#include "ecdsa.h"
#include "log.h"
#include "memzero.h"
#include "secp256k1.h"

#include <stdbool.h>

// This function was copied from the trezor-firmware library, and was not included
// in the initial fork.
// https://github.com/trezor/trezor-firmware/blob/fee0d70211e89629e682f6cda95a83d17ea88825/crypto/ecdsa.c#L1206
//
// ** NOTE **: ONLY USE THIS FUNCTION FOR TESTING.
//
// Parse a DER-encoded signature. We don't check whether the encoded integers
// satisfy DER requirements regarding leading zeros.
static int ecdsa_sig_from_der(const uint8_t* der, size_t der_len, uint8_t sig[64]) {
  memzero(sig, 64);

  // Check sequence header.
  if (der_len < 2 || der_len > 72 || der[0] != 0x30 || der[1] != der_len - 2) {
    return 1;
  }

  // Read two DER-encoded integers.
  size_t pos = 2;
  for (int i = 0; i < 2; ++i) {
    // Check integer header.
    if (der_len < pos + 2 || der[pos] != 0x02) {
      return 1;
    }

    // Locate the integer.
    size_t int_len = der[pos + 1];
    pos += 2;
    if (pos + int_len > der_len) {
      return 1;
    }

    // Skip a possible leading zero.
    if (int_len != 0 && der[pos] == 0) {
      int_len--;
      pos++;
    }

    // Copy the integer to the output, making sure it fits.
    if (int_len > 32) {
      return 1;
    }
    memcpy(sig + 32 * (i + 1) - int_len, der + pos, int_len);

    // Move on to the next one.
    pos += int_len;
  }

  // Check that there are no trailing elements in the sequence.
  if (pos != der_len) {
    return 1;
  }

  return 0;
}

// Returns true if the test vector fails but we intentionally want to ignore.
static bool should_ignore_failure(size_t vector_label) {
  switch (vector_label) {
  case 1:
    // {
    //    "tcId": 1,
    //    "comment": "Signature malleability",
    //    "flags": ["SignatureMalleabilityBitcoin"],
    //    "msg": "313233343030",
    //    "sig":
    //    "3046022100813ef79ccefa9a56f7ba805f0e478584fe5f0dd5f567bc09b5123ccbc9832365022100900e75ad233fcc908509dbff5922647db37c21f4afd3203ae8dc4ae7794b0f87",
    //    "result": "invalid"
    // }

    // The signature is using the high s-value instead of the low one from [bip
    // 62](https://github.com/bitcoin/bips/blob/master/bip-0062.mediawiki) The [code in
    // bitcoin](https://github.com/bitcoin/bitcoin/blob/v0.9.3/src/key.cpp#L202-L227) checks this. if (s > N/2)  { s :=
    // N - s } The group order for secp256k1 is `N = FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFE BAAEDCE6 AF48A03B BFD25E8C
    // D0364141`

    // HalfN := 0x7fffffffffffffffffffffffffffffff5d576e7357a4501ddfe92f46681b20a0
    // s     := 0x900e75ad233fcc908509dbff5922647db37c21f4afd3203ae8dc4ae7794b0f87
    // s > HalfN -> true

  case 100:
    // {
    //    "tcId": 100,
    //    "comment": "truncated r",
    //    "flags": ["ModifiedSignature"],
    //    "msg": "313233343030",
    //    "sig":
    //    "30440220813ef79ccefa9a56f7ba805f0e478584fe5f0dd5f567bc09b5123ccbc983236502206ff18a52dcc0336f7af62400a6dd9b810732baf1ff758000d6f613a556eb31ba",
    //    "result": "invalid"
    // }

    // The DER encoding does not encode leading zeros that it should. Here the signature's r-value is encoded as just a
    // 0x20-byte long bignum, but it should be 0x21-byte long integer with a leading 0x00. The parser is a bit too
    // lenient. However, we are not using this DER parser for anything other than testing.

  case 388:
    // {
    //    "tcId": 388,
    //    "comment": "edge case for signature malleability",
    //    "flags": ["ArithmeticError"],
    //    "msg": "313233343030",
    //    "sig":
    //    "304402207fffffffffffffffffffffffffffffff5d576e7357a4501ddfe92f46681b20a002207fffffffffffffffffffffffffffffff5d576e7357a4501ddfe92f46681b20a1",
    //    "result": "invalid"
    // }

    // This is the same issue as vector 1 as this vector also uses the high s-value.
    // r := 7fffffffffffffffffffffffffffffff5d576e7357a4501ddfe92f46681b20a0
    // s := 7fffffffffffffffffffffffffffffff5d576e7357a4501ddfe92f46681b20a1
    // Note that r = HalfN from case 1 above
    // s is larger than HalfN so this is similar to case 1 and s should be `N - s` instead

    return true;

  default:
    return false;
  }
}

// runs tests against each wycheproof vector and reports any failures
int verify_wycheproof(void) {
// inline the include to have the smallest scope possible for the variables defined in the include file.
#include "wycheproof/ecdsa_secp256k1_sha256_bitcoin_test.h"

  int failures = 0;
  for (size_t t = 0; t < SECP256K1_ECDSA_WYCHEPROOF_NUMBER_TESTVECTORS; t++) {
    // The json test vectors are 1-indexed.
    const size_t vector_label = t + 1;
    const uint8_t* msg = NULL;
    const uint8_t* der_sig = NULL;
    const uint8_t* pk = NULL;
    const ecdsa_curve* curve = &secp256k1;
    uint8_t sig[64] = { 0 };
    curve_point pub;

    pk = &wycheproof_ecdsa_public_keys[testvectors[t].pk_offset];

    int is_valid_pubkey = ecdsa_read_pubkey(curve, pk, &pub);
    if (is_valid_pubkey == 0) {
      ERROR("pub key not valid for test vector %zu", vector_label);
      return -1;
    }

    msg = &wycheproof_ecdsa_messages[testvectors[t].msg_offset];
    der_sig = &wycheproof_ecdsa_signatures[testvectors[t].sig_offset];

    // returns non-zero if parsing fails, ignore the return value and continue with the verification, and
    // in an actual verification function, we would bail if we found a problem here.
    ecdsa_sig_from_der(der_sig, testvectors[t].sig_len, sig);

    // ecdsa_verify returns 0 if verification succeeds.
    int failed_verify = ecdsa_verify(curve, HASHER_SHA2, pk, sig, msg, testvectors[t].msg_len);

    // convert ecdsa_verify to match our test vectors. 0 = success, !0 = invalid.
    int actual_verify = (failed_verify == 0) ? 1 : 0;

    if (testvectors[t].expected_verify != actual_verify) {
      if (should_ignore_failure(vector_label)) {
        // This is a known failure, we ignore.
        continue;
      }

      ERROR(
          "wycheproof test vector failed [vector:%zu][expected: %d][actual: %d]",
          vector_label,
          testvectors[t].expected_verify,
          actual_verify);

      failures++;
    }
  }

  return failures;
}
