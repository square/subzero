#pragma once

#include "squareup/subzero/internal.pb.h"

#include <stdbool.h>

Result handle_sign_tx(
    const InternalCommandRequest_SignTxRequest* const request,
    InternalCommandResponse_SignTxResponse *response);

bool validate_fees(const InternalCommandRequest_SignTxRequest* const request);
