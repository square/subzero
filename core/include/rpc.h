#pragma once

#include "nanopb_stream.h"
#include <squareup/plutus/internal.pb.h>

void handle_incoming_message(pb_istream_t *input, pb_ostream_t *output);

Result handle_init_wallet(InternalCommandRequest *,
                          InternalCommandResponse_InitWalletResponse *);

Result handle_finalize_wallet(InternalCommandRequest_FinalizeWalletRequest *,
                              InternalCommandResponse_FinalizeWalletResponse *);

Result pre_execute_command(InternalCommandRequest *in);
void post_execute_command(void);
