#pragma once

#include "config.h"
#include "squareup/subzero/internal.pb.h"

#include <stdint.h>

Result mix_entropy(uint8_t wallet_entropy[static MASTER_SEED_SIZE], const InternalCommandRequest* const in);
