#include <assert.h>
#include <protection.h>
#include <squareup/subzero/common.pb.h>
#include <squareup/subzero/internal.pb.h>

#include "log.h"
#include "memzero.h"
#include "aes_gcm_dev.h"
#include "rand.h"

// Protection: developer edition.

// Hardcoded master seed encryption key and public key encryption key
// From test cases 1 and 3 in
// https://csrc.nist.rip/groups/ST/toolkit/BCM/documents/proposedmodes/gcm/gcm-spec.pdf
uint8_t KEK[2][16] =
{
  {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
  {0xfe, 0xff, 0xe9, 0x92, 0x86, 0x65, 0x73, 0x1c,
   0x6d, 0x6a, 0x8f, 0x94, 0x67, 0x30, 0x83, 0x08}
};

// The following implementation is shared between dev and ncipher targets
#include "../protect.c"
