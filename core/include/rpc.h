#pragma once

#include "pb.h"
#include "squareup/subzero/internal.pb.h"

void handle_incoming_message(pb_istream_t* input, pb_ostream_t* output);

Result handle_init_wallet(const InternalCommandRequest* const in, InternalCommandResponse_InitWalletResponse* out);

Result handle_finalize_wallet(
    const InternalCommandRequest_FinalizeWalletRequest* const in,
    InternalCommandResponse_FinalizeWalletResponse* out);

Result pre_execute_command(const InternalCommandRequest* const in);
void post_execute_command(void);
