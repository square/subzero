#include "checks.h"
#include "config.h"
#include "log.h"
#include "protection.h"

int verify_protect_pubkey(void) {
  EncryptedPubKey temp = EncryptedPubKey_init_default;
  char buffer1[XPUB_SIZE] =
      "xpub661MyMwAqRbcGw6rpZ6SYUfFk6Z5YX216YRXnhuB6UcdwuVe4XUKKiPg";
  char buffer2[XPUB_SIZE];
  strcpy(buffer2, buffer1);

  char buffer3[XPUB_SIZE];

  Result r = protect_pubkey(buffer1, &temp);
  if (r != Result_SUCCESS) {
    ERROR("protect_pubkey failed: (%d).", r);
    return -1;
  }
  r = expose_pubkey(&temp, buffer3);
  if (r != Result_SUCCESS) {
    ERROR("expose_pubkey failed: (%d).", r);
    return -1;
  }

  if (strcmp(buffer2, buffer3) != 0) {
    ERROR("strcmp failed. %s != %s", buffer2, buffer3);
    return -1;
  }

  INFO("verify_protect_pubkey: ok");
  return 0;
}
