#include "checks.h"
#include "config.h"
#include "init_wallet.h"
#include "log.h"
#include "squareup/subzero/internal.pb.h"

#include <stdint.h>
#include <string.h>

int verify_mix_entropy(void) {
  // create the first buffer with known bytes
  uint8_t buffer1[] = {
      0x75, 0x96, 0x8d, 0x5b, 0xc9, 0x4d, 0x5c, 0x9c,
      0xda, 0xe8, 0xdf, 0x35, 0xb7, 0x4d, 0x62, 0x28,
      0xd4, 0xbf, 0x5c, 0x2f, 0x4f, 0x89, 0x60, 0x54,
      0x0c, 0x4d, 0x26, 0x96, 0x7b, 0x60, 0x86, 0x4b,
      0x3f, 0x3e, 0xf1, 0x2f, 0x43, 0x5f, 0x04, 0xa4,
      0xf5, 0xc0, 0xb5, 0x17, 0xe0, 0x5b, 0xa1, 0xf9,
      0x29, 0x81, 0x33, 0xc0, 0x74, 0x12, 0x07, 0x72,
      0x5d, 0xdd, 0xba, 0xdf, 0x3c, 0xdc, 0x0c, 0xe9};

  // create second buffer
  uint8_t buffer2[] = {
      0xe4, 0x88, 0xa6, 0x4e, 0xd9, 0xca, 0x63, 0xcb,
      0x3f, 0xca, 0x7f, 0x00, 0xda, 0xf9, 0x6f, 0xab,
      0x18, 0x23, 0x54, 0x0b, 0x1d, 0x10, 0xfa, 0xff,
      0x57, 0xbe, 0x00, 0xcc, 0x83, 0xdf, 0x03, 0x66,
      0xc5, 0x5b, 0x82, 0xe8, 0x46, 0x6d, 0x7d, 0x31,
      0x7d, 0xdd, 0x92, 0xda, 0x75, 0x38, 0xea, 0x91,
      0x23, 0x75, 0xa0, 0xae, 0x5a, 0xfb, 0x8a, 0x2b,
      0x93, 0xa9, 0x3d, 0xd7, 0x4e, 0x40, 0x62, 0x62};
  InternalCommandRequest in = InternalCommandRequest_init_default;
  in.which_command = InternalCommandRequest_InitWallet_tag;

  memcpy(in.command.InitWallet.random_bytes.bytes, buffer2, sizeof(buffer2));
  in.command.InitWallet.random_bytes.size = sizeof(buffer2);

  // call mix_entropy
  if (mix_entropy(buffer1, &in) != Result_SUCCESS) {
    ERROR("mix_entropy failed");
    return -1;
  }

  // verify result
  uint8_t buffer3[MASTER_SEED_SIZE] = {
      0x91, 0x1e, 0x2b, 0x15, 0x10, 0x87, 0x3f, 0x57,
      0xe5, 0x22, 0xa0, 0x35, 0x6d, 0xb4, 0x0d, 0x83,
      0xcc, 0x9c, 0x08, 0x24, 0x52, 0x99, 0x9a, 0xab,
      0x5b, 0xf3, 0x26, 0x5a, 0xf8, 0xbf, 0x85, 0x2d,
      0xfa, 0x65, 0x73, 0xc7, 0x05, 0x32, 0x79, 0x95,
      0x88, 0x1d, 0x27, 0xcd, 0x95, 0x63, 0x4b, 0x68,
      0x0a, 0xf4, 0x93, 0x6e, 0x2e, 0xe9, 0x8d, 0x59,
      0xce, 0x74, 0x87, 0x08, 0x72, 0x9c, 0x6e, 0x8b};

  if (memcmp(buffer1, buffer3, MASTER_SEED_SIZE) != 0) {
    ERROR("mix_entropy failed to return expected value");
    return -1;
  }

  INFO("verify_mix_entropy: ok");
  return 0;
}
