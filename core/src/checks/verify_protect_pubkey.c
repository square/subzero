#include "aes_gcm_common.h"
#include "checks.h"
#include "config.h"
#include "log.h"
#include "protection.h"
#include "squareup/subzero/common.pb.h"
#include "squareup/subzero/internal.pb.h"
#include "strlcpy.h"

#include <string.h>

int verify_protect_pubkey(void) {
  EncryptedPubKey temp = EncryptedPubKey_init_default;
  char buffer1[XPUB_SIZE] =
      "xpub661MyMwAqRbcGw6rpZ6SYUfFk6Z5YX216YRXnhuB6UcdwuVe4XUKKiPg";
  char buffer2[XPUB_SIZE];
  subzero_strlcpy(buffer2, buffer1, sizeof(buffer2));

  char buffer3[XPUB_SIZE];

  // Negative test for null pubkey input
  ERROR("(next line is expected to show red text...)");
  Result r = protect_pubkey(buffer1, NULL);
  if (Result_UNKNOWN_INTERNAL_FAILURE != r) {
    ERROR("%s: unexected result %d from protect_pubkey() with NULL pubkey pointer", __func__, r);
    return -1;
  }

  // Positive test case
  r = protect_pubkey(buffer1, &temp);
  if (r != Result_SUCCESS) {
    ERROR("protect_pubkey failed: (%d).", r);
    return -1;
  }

  // Make sure the plaintext buffer has been zeroed.
  char zerobuffer[sizeof(buffer1)] = { 0 };
  if (0 != memcmp(zerobuffer, buffer1, sizeof(buffer1))) {
    ERROR("%s: plaintext buffer not zero after encrypt op", __func__);
    return -1;
  }

  // Negative test cases.
  ERROR("(next line is expected to show red text...)");
  r = expose_pubkey(NULL, buffer3);
  if (Result_UNKNOWN_INTERNAL_FAILURE != r) {
    ERROR("%s: unexected result %d from expose_pubkey() with NULL pubkey pointer", __func__, r);
    return -1;
  }

  temp.has_encrypted_pub_key = false;
  ERROR("(next line is expected to show red text...)");
  r = expose_pubkey(&temp, buffer3);
  if (Result_EXPOSE_PUBKEY_NO_ENCRYPTED_PUBKEY_FAILURE != r) {
    ERROR("%s: unexected result %d from expose_pubkey() with unset encrypted pub key", __func__, r);
    return -1;
  }
  temp.has_encrypted_pub_key = true;

  size_t oldsize = temp.encrypted_pub_key.size;
  temp.encrypted_pub_key.size = XPUB_SIZE + AES_GCM_OVERHEAD_BYTES;
  ERROR("(next line is expected to show red text...)");
  r = expose_pubkey(&temp, buffer3);
  if (Result_EXPOSE_PUBKEY_UNEXPECTED_ENCRYPTED_PUBKEY_SIZE_FAILURE != r) {
    ERROR("%s: unexected result %d from expose_pubkey() with wrong size encrypted pub key", __func__, r);
    return -1;
  }
  temp.encrypted_pub_key.size = oldsize;

  temp.encrypted_pub_key.bytes[0] ^= 1; // flip a ciphertext bit
  ERROR("(next line is expected to show red text...)");
  r = expose_pubkey(&temp, buffer3);
  if (Result_SUCCESS == r) {
    ERROR("%s: unexected result %d from expose_pubkey() with corrupted ciphertext", __func__, r);
    return -1;
  }
  temp.encrypted_pub_key.bytes[0] ^= 1; // flip the bit back

  // Positive test case.
  r = expose_pubkey(&temp, buffer3);
  if (r != Result_SUCCESS) {
    ERROR("expose_pubkey failed: (%d).", r);
    return -1;
  }

  if (strcmp(buffer2, buffer3) != 0) {
    ERROR("%s: decrypted pubkey does not match original input: %s != %s", __func__, buffer2, buffer3);
    return -1;
  }

  INFO("verify_protect_pubkey: ok");
  return 0;
}
