#include <stdio.h>

#include <pb_decode.h>
#include <pb_encode.h>
#include <squareup/subzero/internal.pb.h>
#include <squareup/subzero/service.pb.h>

#include "checks.h"
#include "config.h"
#include "log.h"
#include "rpc.h"
#include "sign.h"
#include "qrsignatures.h"

static void execute_command(InternalCommandRequest *,
                            InternalCommandResponse *);

static int check_version(InternalCommandRequest *cmd) {
  if (VERSION != cmd->version) {
    ERROR("Version mismatch. Expecting %d, got %d.", VERSION, cmd->version);
    return false;
  }
  return true;
}
/**
 * Check signature on input bytes.
 * Deserialize them and then populate the internal command request's
 * redundant fields with the deserialized data.
 * @return Return an error if the signature check failed or decoding the bytes failed.
 */
static Result populate_internal_command(InternalCommandRequest * to){
  
  Result res = Result_SUCCESS;
  // The signature being checked here is on the serialized bytes generated by the coordinator service.
  // Those bytes are the serialization of CommandRequest structure.
  // Because protobuf is not determinstic so the bytes for CommandRequest generated by the coordinator
  // service are golden. The same bytes are used here as well.
  if(!check_qrsignature(to->serialized_command_request.bytes, to->serialized_command_request.size, to->qrsignature.signature.bytes)){
    res = Result_QRSIG_CHECK_FAILED;
    ERROR("Signature check on the binary payload failed.");
    goto cleanup;
  
  }

  pb_istream_t pb_command = pb_istream_from_buffer(to->serialized_command_request.bytes, to->serialized_command_request.size);
  CommandRequest from = CommandRequest_init_default;
  // Cannot use pb_decode_delimited as on the coordinator service the java code is generating bytes
  // without the delimited values.
  if(!pb_decode(&pb_command, CommandRequest_fields, &from)){
    res = Result_COMMAND_DECODE_FAILED;
    ERROR("Could not decode input bytes for command request.");
    goto cleanup;
  }
  if(from.which_command != CommandRequest_SignTx_tag){
    res = Result_COMMAND_DECODE_FAILED;
    ERROR("Input command is not Sign tx");
    goto cleanup;
  }  
  to->command.SignTx.inputs_count = from.command.SignTx.inputs_count;
  to->command.SignTx.outputs_count = from.command.SignTx.outputs_count;
  for(pb_size_t i = 0; i < from.command.SignTx.inputs_count ; i++){
      to->command.SignTx.inputs[i] = from.command.SignTx.inputs[i];
      
  }
  for(pb_size_t i = 0; i < from.command.SignTx.outputs_count ; i++){
      to->command.SignTx.outputs[i] = from.command.SignTx.outputs[i];
  }
 
  //lock time is used in internal hash calculation so it should be present.
  if (from.command.SignTx.has_lock_time) {
    to->command.SignTx.lock_time = from.command.SignTx.lock_time;
  } else {
    ERROR("Lock time not present in deserialized bytes.");
    res = Result_REQUIRED_FIELDS_NOT_PRESENT;
    goto cleanup;
  }
  //Wallet ID is optional and not really used in the code at the moment.
  if (from.has_wallet_id) {
    to->wallet_id = from.wallet_id;
  }

  cleanup:
  
  return res;
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
  // Only parse the binary blob if it's present and only for sign transaction.
  // We could enable this code for init wallet/finalize wallet as well at a later point.
  // for backwards compatibility.
  if (cmd.has_serialized_command_request && cmd.which_command == InternalCommandRequest_SignTx_tag) {
    Result res = populate_internal_command(&cmd);
    if (res != Result_SUCCESS) {
      ERROR("Populating internal command with authenticated payload failed.");
      out.which_response = InternalCommandResponse_Error_tag;
      out.response.Error.code = res;
      snprintf(out.response.Error.message, sizeof(out.response.Error.message),
              "Populating internal command with authenticated payload failed.");
      out.response.Error.has_message = true;
      if (!pb_encode_delimited(output, InternalCommandResponse_fields, &out)) {
        ERROR("Encoding error message about encoding failed: %s",
              PB_GET_ERROR(output));
      }
    return;     
    }
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
