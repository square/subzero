#pragma once

#include <squareup/subzero/common.pb.h>
#include <squareup/subzero/internal.pb.h>
#include <stdint.h>

#include "config.h"

Result mix_entropy(uint8_t wallet_entropy[static MASTER_SEED_SIZE],
                   const InternalCommandRequest* const in);
