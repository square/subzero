#pragma once

#include <squareup/plutus/common.pb.h>
#include <squareup/plutus/internal.pb.h>
#include <stdint.h>

#include "config.h"

Result mix_entropy(uint8_t wallet_entropy[static MASTER_SEED_SIZE],
                   InternalCommandRequest *in);
