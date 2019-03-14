#include <stdio.h>

#include <pb_decode.h>
#include <pb_encode.h>
#include <squareup/subzero/internal.pb.h>

#include "checks.h"
#include "config.h"
#include "log.h"
#include "rpc.h"
#include "sign.h"

static void execute_command(InternalCommandRequest *,
                            InternalCommandResponse *);

static int check_version(InternalCommandRequest *cmd) {
  if (VERSION != cmd->version) {
    ERROR("Version mismatch. Expecting %d, got %d.", VERSION, cmd->version);
    return false;
  }
  return true;
}

// handle_incoming_message is the central RPC entry point that invokes the
// requested command.
void handle_incoming_message(pb_istream_t *input, pb_ostream_t *output) {
  InternalCommandRequest cmd = InternalCommandRequest_init_default;
  InternalCommandResponse out = InternalCommandResponse_init_default;

  if (!pb_decode_delimited(input, InternalCommandRequest_fields, &cmd)) {
    ERROR("Decode failed: %s", PB_GET_ERROR(input));
    out.which_response = InternalCommandResponse_Error_tag;
    out.response.Error.code = Result_COMMAND_DECODE_FAILED;
    snprintf(out.response.Error.message, sizeof(out.response.Error.message),
             "Error decoding: %s", PB_GET_ERROR(input));
    out.response.Error.has_message = true;
    if (!pb_encode_delimited(output, InternalCommandResponse_fields, &out)) {
      ERROR("Encoding error message about decoding failed: %s",
            PB_GET_ERROR(output));
    }
    return;
  }

  execute_command(&cmd, &out);

  if (!pb_encode_delimited(output, InternalCommandResponse_fields, &out)) {
    ERROR("Encoding failed: %s", PB_GET_ERROR(output));
    out.which_response = InternalCommandResponse_Error_tag;
    out.response.Error.code = Result_COMMAND_ENCODE_FAILED;
    snprintf(out.response.Error.message, sizeof(out.response.Error.message),
             "Error encoding: %s", PB_GET_ERROR(output));
    out.response.Error.has_message = true;
    if (!pb_encode_delimited(output, InternalCommandResponse_fields, &out)) {
      ERROR("Encoding error message about encoding failed: %s",
            PB_GET_ERROR(output));
    }
    return;
  }
  DEBUG("done: %zd", output->bytes_written);
}

// execute command
static void execute_command(InternalCommandRequest *cmd,
                            InternalCommandResponse *out) {
  if (!check_version(cmd)) {
    out->which_response = InternalCommandResponse_Error_tag;
    out->response.Error.code = Result_VERSION_MISMATCH;
    return;
  }

  Result r = pre_execute_command(cmd);
  if (r != Result_SUCCESS) {
    ERROR("pre_execute_command failed");
    out->which_response = InternalCommandResponse_Error_tag;
    out->response.Error.code = r;
    return;
  }

  switch (cmd->which_command) {
  case InternalCommandRequest_InitWallet_tag:
    INFO("Command InitWallet");
    r = handle_init_wallet(cmd, &out->response.InitWallet);
    out->which_response = InternalCommandResponse_InitWallet_tag;
    break;
  case InternalCommandRequest_FinalizeWallet_tag:
    INFO("Command FinalizeWallet");
    r = handle_finalize_wallet(&cmd->command.FinalizeWallet,
                               &out->response.FinalizeWallet);
    out->which_response = InternalCommandResponse_FinalizeWallet_tag;
    break;
  case InternalCommandRequest_SignTx_tag:
    INFO("Command SignTx");
    r = handle_sign_tx(&cmd->command.SignTx, &out->response.SignTx);
    out->which_response = InternalCommandResponse_SignTx_tag;
    break;
  default:
    ERROR("Unknown command: %d", cmd->which_command);
    r = Result_UNKNOWN_COMMAND;
  }

  post_execute_command();

  if (r != Result_SUCCESS) {
    out->which_response = InternalCommandResponse_Error_tag;
    out->response.Error.code = r;
    snprintf(out->response.Error.message, sizeof(out->response.Error.message),
             "Error handling command tag %d; r = %d\n", cmd->which_command, r);
    ERROR("%s", out->response.Error.message);
    out->response.Error.has_message = true;
  }
}
