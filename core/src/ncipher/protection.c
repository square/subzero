#include <assert.h>
#include <nfastapp.h>
#include <protection.h>
#include <seelib.h>
#include <squareup/subzero/common.pb.h>
#include <squareup/subzero/internal.pb.h>

#include "aes_gcm.h"
#include "config.h"
#include "log.h"

// Protection: ncipher edition.

M_KeyID master_seed_encryption_key = 0;
M_KeyID pub_key_encryption_key = 0;

// The following implementation is shared between dev and ncipher targets
#include "../protect.c"

