#include <assert.h>
#include <nfastapp.h>
#include <seelib.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "check_ver.h"
#include "config.h"
#include "log.h"

static void check_ver_write(void);
static int check_ver_read(void);

extern NFastApp_Connection conn;
extern NFast_AppHandle app;

/**
 * check_ver() provides rollback protection. The code prevents using older
 * versions of signed CodeSafe code after a newer version has been deployed. We
 * thus limit the attack surface to only the latest version of our code, as
 * opposed to any version which has ever been signed.
 *
 * Rollback protection works by storing a magic number & a version number in the
 * HSMs NVRAM. The NVRAM is protected using ACLs, so only the Codesafe code can
 * write to it.
 *
 * The magic number prevents swapping NVRAM between applications. For instance,
 * CodeCassone as its own magic number. The version number is a high water mark,
 * used to track which versions of the signed code have been seen.
 *
 * Creating a fresh NVRAM requires an ACS quorum.
 * TODO: document NVRAM creation + ACL setup process.
 * TODO: experiment with the INCR ACL.
 *
 * Note: this code is very similar to rollback.c, currently being added
 * CodeCassone. The nCipher API we use however differs.
 * https://stash.corp.squareup.com/projects/INFOSEC/repos/codecassone/commits/f5495b09f511c797b49b04084d9b65101b060efc#module-utac/rollback.c
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
 * TODO: we could put check_ver() in src/ and have check_ver_read() +
 * check_ver_write() live in the dev/ + ncipher/ folders.
 */

void check_ver() {
  int version;

  version = check_ver_read();
  if (version == -1) {
    ERROR("check_ver: check_ver_read() failed. Exiting.");
    exit(-1);
  }
  DEBUG("check_ver: NVRAM version is %d.", version);

  if (version > VERSION) {
    ERROR("check_ver: rollback detected! Exiting");
    exit(-1);
  } else if (version < VERSION) {
    ERROR("check_ver: updating version stored in NVRAM.");
    check_ver_write();
    // todo: we should re-read!
  } else {
    assert(version == VERSION);
    INFO("check_ver: version match.");
  }
}

// For some unknown reason, NFastApp_Transact with -O2 requires
// heap allocated buffers.
uint8_t check_ver_buf[VERSION_SIZE] = {0};

static void check_ver_write() {
  M_Command command = {0};
  M_Reply reply = {0};
  M_Status rc;

  char file_name[] = "test-file";
  command.cmd = Cmd_NVMemOp;
  command.args.nvmemop.module = 1; // we assume there's only HSM.
  command.args.nvmemop.op = NVMemOpType_Write;
  // TODO: assert file_name is <= 10 bytes + NULL
  memcpy(&(command.args.nvmemop.name), &file_name, strlen(file_name));

  snprintf((char *)check_ver_buf, sizeof(check_ver_buf), "%d-%d", VERSION_MAGIC,
           VERSION);

  command.args.nvmemop.val.write.data.len = sizeof(check_ver_buf);
  command.args.nvmemop.val.write.data.ptr = check_ver_buf;

  if ((rc = NFastApp_Transact(conn, NULL, &command, &reply, NULL)) !=
      Status_OK) {
    ERROR("check_ver_write: NFastApp_Transact failed (%s).",
          NF_Lookup(rc, NF_Status_enumtable));
    goto exit;
  }

  if (reply.status != Status_OK) {
    ERROR("check_ver_write: NFastApp_Transact returned error (%d).",
          reply.status);
    goto exit;
  }
  INFO("check_ver_write: write success");

exit:
  NFastApp_Free_Reply(app, NULL, NULL, &reply);
}

static int check_ver_read() {
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
    ERROR("check_ver_read: NFastApp_Transact failed (%s).",
          NF_Lookup(rc, NF_Status_enumtable));
    goto exit;
  }

  if (reply.status != Status_OK) {
    ERROR("check_ver_read: NFastApp_Transact returned error (%d).",
          reply.status);
    goto exit;
  }

  // Validate magic string and return the version
  DEBUG("check_ver_read: nvram contents: (%d) ",
        reply.reply.nvmemop.res.read.data.len);
  for (unsigned int i = 0; i < reply.reply.nvmemop.res.read.data.len; i++) {
    DEBUG_("%02x", reply.reply.nvmemop.res.read.data.ptr[i]);
  }
  DEBUG_("\n");

  int magic;
  if (sscanf((const char *)reply.reply.nvmemop.res.read.data.ptr, "%u-%u",
             &magic, &version) != 2) {
    ERROR("check_ver_read: failed to parse nvram");
    version = -1;
    goto exit;
  }
  if (magic != VERSION_MAGIC) {
    ERROR("check_ver_read: unexpected magic number (%d)", magic);
    version = -1;
    goto exit;
  }

exit:
  NFastApp_Free_Reply(app, NULL, NULL, &reply);
  return version;
}
