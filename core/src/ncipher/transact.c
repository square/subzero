#include <nfastapp.h>

#include "log.h"
#include "transact.h"

/**
 * Sets certs_present and takes care of error handling.
 *
 * It is unclear if we need to call NFastApp_Free_Command and NFastApp_Free_Reply for stack allocated buffers. In either
 * case, that would happen in the caller.
 */
extern NFastApp_Connection conn;
extern NFast_AppHandle app;
extern M_CertificateList cert_list;

Result transact(M_Command *command, M_Reply *reply) {
  if (command->cmd != Cmd_GetWorldSigners) {
    // Set certs_present, unless we are asking the HSM for the certs in the first place.
    command->certs = &cert_list;
    command->flags |= Command_flags_certs_present;
  }

  M_Status rc;
  if ((rc = NFastApp_Transact(conn, NULL, command, reply, NULL)) != Status_OK) {
    ERROR("transact: NFastApp_Transact failed (%s).", NF_Lookup(rc, NF_Status_enumtable));
    return Result_NFAST_APP_TRANSACT_FAILURE;
  }
  if (reply->status != Status_OK) {
    char buf[1000];
    NFast_StrError(buf, sizeof(buf), reply->status, NULL);
    ERROR("transact: NFastApp_Transact returned error (%d) (%s).", reply->status, buf);
    return Result_NFAST_APP_TRANSACT_STATUS_FAILURE;
  }
  return Result_SUCCESS;
}
