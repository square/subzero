#include "no_rollback.h"

#include "config.h"
#include "log.h"
#include "transact.h"

#include <nfastapp.h>
#include <string.h>

extern NFast_AppHandle app;

/**
 * Creating a fresh NVRAM requires an ACS quorum. The easiest way to setup the
 * NVRAM is to use the Java GUI application with --init-nvram.
 *
 * Note: we decided not to use the INCR instruction. We feel it's overly complicated.
 *
 * For testing purposes, the following commands can be used to delete or allocate the NVRAM.
 * By default, the NVRAM is 100 bytes:
 * /opt/nfast/bin/nvram-sw -a -n "subzero0001" (or -n "selfcheck01")
 * /opt/nfast/bin/nvram-sw -d -n "subzero0001" (or -n "selfcheck01")
 *
 * To initialize the NVRAM to some initial state:
 * printf "%-99s %s" "8414-100" | tr ' ' '\0' > nvram
 * /opt/nfast/bin/nvram-sw --write -f nvram -n "subzero0001" (or -n "selfcheck01")
 * /opt/nfast/bin/nvram-sw --read -n "subzero0001" (or -n "selfcheck01") | xxd
 */

Result no_rollback_read(const char* filename, char buf[static VERSION_SIZE]) {
  Result r = Result_UNKNOWN_INTERNAL_FAILURE;

  M_Command command = {0};
  M_Reply reply = {0};

  command.cmd = Cmd_NVMemOp;
  command.args.nvmemop.module = 1; // we assume there's only HSM.
  command.args.nvmemop.op = NVMemOpType_Read;
  memcpy(&(command.args.nvmemop.name), filename, strlen(filename));

  r = transact(&command, &reply);
  if (r != Result_SUCCESS) {
    ERROR("no_rollback_read: transact failed.");
    NFastApp_Free_Reply(app, NULL, NULL, &reply);
    return r;
  }

  // Validate magic string and return the version
  DEBUG("no_rollback_read: nvram contents: (%d) ",
        reply.reply.nvmemop.res.read.data.len);
  for (unsigned int i = 0; i < reply.reply.nvmemop.res.read.data.len; i++) {
    DEBUG_("%02x", reply.reply.nvmemop.res.read.data.ptr[i]);
  }
  DEBUG_("\n");
  if (reply.reply.nvmemop.res.read.data.len != VERSION_SIZE) {
    ERROR("Expecting NVRAM size to be %d, got %d", VERSION_SIZE, reply.reply.nvmemop.res.read.data.len);
    NFastApp_Free_Reply(app, NULL, NULL, &reply);
    return Result_UNKNOWN_INTERNAL_FAILURE;
  }

  if (reply.reply.nvmemop.res.read.data.ptr[VERSION_SIZE - 1] != '\0') {
    ERROR("%s: rollback NVRAM contents not NULL-terminated", __func__);
    NFastApp_Free_Reply(app, NULL, NULL, &reply);
    return Result_NO_ROLLBACK_INVALID_FORMAT;
  }

  memcpy(buf, reply.reply.nvmemop.res.read.data.ptr, VERSION_SIZE);
  NFastApp_Free_Reply(app, NULL, NULL, &reply);
  return Result_SUCCESS;
}

Result no_rollback_write(const char* filename, char buf[static VERSION_SIZE]) {
  if (buf[VERSION_SIZE - 1] != '\0') {
    ERROR("%s: input buf is not NULL-terminated", __func__);
    return Result_NO_ROLLBACK_INVALID_FORMAT;
  }

  Result r = Result_UNKNOWN_INTERNAL_FAILURE;

  M_Command command = {0};
  M_Reply reply = {0};

  command.cmd = Cmd_NVMemOp;
  command.args.nvmemop.module = 1; // we assume there's only one HSM.
  command.args.nvmemop.op = NVMemOpType_Write;
  // TODO: assert file_name is <= 10 bytes + NULL
  memcpy(&(command.args.nvmemop.name), filename, strlen(filename));

  command.args.nvmemop.val.write.data.len = VERSION_SIZE;
  command.args.nvmemop.val.write.data.ptr = (unsigned char*) buf;

  r = transact(&command, &reply);
  if (r != Result_SUCCESS) {
    ERROR("no_rollback_write: transact failed.");
    NFastApp_Free_Reply(app, NULL, NULL, &reply);
    return r;
  }
  INFO("no_rollback_write: write success");

  NFastApp_Free_Reply(app, NULL, NULL, &reply);
  return Result_SUCCESS;
}
