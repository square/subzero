#include <assert.h>
#include <nfastapp.h>
#include <seelib.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "no_rollback.h"
#include "config.h"
#include "log.h"

static void no_rollback_write(void);
static int no_rollback_read(void);

extern NFastApp_Connection conn;
extern NFast_AppHandle app;

/**
 * no_rollback() provides rollback protection. The code prevents using older
 * versions of signed CodeSafe code after a newer version has been deployed. We
 * thus limit the attack surface to only the latest version of our code, as
 * opposed to any version which has ever been signed.
 *
 * Rollback protection works by storing a magic number & a version number in the
 * HSMs NVRAM. The NVRAM is protected using ACLs, so only the Codesafe code can
 * write to it.
 *
 * The magic number prevents swapping NVRAM between applications. The version
 * number is a high water mark, used to track which versions of the signed code
 * have been seen.
 *
 * Creating a fresh NVRAM requires an ACS quorum.
 * TODO: document NVRAM creation + ACL setup process.
 * TODO: experiment with the INCR ACL.
 *
 * To delete or allocate the NVRAM. By default, the NVRAM is 100 bytes:
 * /opt/nfast/bin/nvram-sw -d
 * /opt/nfast/bin/nvram-sw -a
 *
 * To initialize the NVRAM to some initial state:
 * printf "%-99s %s" "8414-100" | tr ' ' '\0' > nvram
 * /opt/nfast/bin/nvram-sw --write -m 1 -f nvram
 * /opt/nfast/bin/nvram-sw --read | xxd
 *
 * TODO: write some tests
 * - ensure code fails if the MAGIC number mismatches
 * - ensure code works and upgrades if the version number is smaller.
 * - ensure code works if the version number is an exact match
 * - ensure code fails if the version number is greater.
 *
 * TODO: we could put no_rollback() in src/ and have no_rollback_read() +
 * no_rollback_write() live in the dev/ + ncipher/ folders.
 */

Result no_rollback() {
  int version;

  version = no_rollback_read();
  if (version == -1) {
    ERROR("no_rollback: no_rollback_read() failed. Exiting.");
    // TODO: convert no_rollback_read to return a Result
    return Result_NO_ROLLBACK_INVALID_VERSION;
  }
  DEBUG("no_rollback: NVRAM version is %d.", version);

  if (version > VERSION) {
    ERROR("no_rollback: rollback detected! Exiting");
    return Result_NO_ROLLBACK_INVALID_VERSION;
  } else if (version < VERSION) {
    ERROR("no_rollback: updating version stored in NVRAM.");
    no_rollback_write();
    // todo: we should re-read!
  } else {
    assert(version == VERSION);
    INFO("no_rollback: version match.");
  }
  return Result_SUCCESS;
}

// For some unknown reason, NFastApp_Transact with -O2 requires
// heap allocated buffers.
uint8_t no_rollback_buf[VERSION_SIZE] = {0};

static void no_rollback_write() {
  M_Command command = {0};
  M_Reply reply = {0};
  M_Status rc;

  char file_name[] = "test-file";
  command.cmd = Cmd_NVMemOp;
  command.args.nvmemop.module = 1; // we assume there's only HSM.
  command.args.nvmemop.op = NVMemOpType_Write;
  // TODO: assert file_name is <= 10 bytes + NULL
  memcpy(&(command.args.nvmemop.name), &file_name, strlen(file_name));

  snprintf((char *)no_rollback_buf, sizeof(no_rollback_buf), "%d-%d", VERSION_MAGIC,
           VERSION);

  command.args.nvmemop.val.write.data.len = sizeof(no_rollback_buf);
  command.args.nvmemop.val.write.data.ptr = no_rollback_buf;

  if ((rc = NFastApp_Transact(conn, NULL, &command, &reply, NULL)) !=
      Status_OK) {
    ERROR("no_rollback_write: NFastApp_Transact failed (%s).",
          NF_Lookup(rc, NF_Status_enumtable));
    goto exit;
  }

  if (reply.status != Status_OK) {
    ERROR("no_rollback_write: NFastApp_Transact returned error (%d).",
          reply.status);
    goto exit;
  }
  INFO("no_rollback_write: write success");

exit:
  NFastApp_Free_Reply(app, NULL, NULL, &reply);
}

static int no_rollback_read() {
  int version = -1;

  M_Command command = {0};
  M_Reply reply = {0};
  M_Status rc;

  char file_name[] = "test-file";
  command.cmd = Cmd_NVMemOp;
  command.args.nvmemop.module = 1; // we assume there's only HSM.
  command.args.nvmemop.op = NVMemOpType_Read;
  memcpy(&(command.args.nvmemop.name), &file_name, strlen(file_name));

  if ((rc = NFastApp_Transact(conn, NULL, &command, &reply, NULL)) !=
      Status_OK) {
    ERROR("no_rollback_read: NFastApp_Transact failed (%s).",
          NF_Lookup(rc, NF_Status_enumtable));
    goto exit;
  }

  if (reply.status != Status_OK) {
    ERROR("no_rollback_read: NFastApp_Transact returned error (%d).",
          reply.status);
    goto exit;
  }

  // Validate magic string and return the version
  DEBUG("no_rollback_read: nvram contents: (%d) ",
        reply.reply.nvmemop.res.read.data.len);
  for (unsigned int i = 0; i < reply.reply.nvmemop.res.read.data.len; i++) {
    DEBUG_("%02x", reply.reply.nvmemop.res.read.data.ptr[i]);
  }
  DEBUG_("\n");

  int magic;
  if (sscanf((const char *)reply.reply.nvmemop.res.read.data.ptr, "%u-%u",
             &magic, &version) != 2) {
    ERROR("no_rollback_read: failed to parse nvram");
    version = -1;
    goto exit;
  }
  if (magic != VERSION_MAGIC) {
    ERROR("no_rollback_read: unexpected magic number (%d)", magic);
    version = -1;
    goto exit;
  }

exit:
  NFastApp_Free_Reply(app, NULL, NULL, &reply);
  return version;
}
