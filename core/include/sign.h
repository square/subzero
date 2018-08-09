#pragma once

Result handle_sign_tx(InternalCommandRequest_SignTxRequest *,
                      InternalCommandResponse_SignTxResponse *);

bool validate_fees(InternalCommandRequest_SignTxRequest *request);
