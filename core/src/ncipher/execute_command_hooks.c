#include <assert.h>
#include <examples/network_server/common.h>
#include <nfastapp.h>
#include <pb_decode.h>
#include <pb_encode.h>
#include <seelib.h>
#include <squareup/subzero/common.pb.h>
#include <squareup/subzero/internal.pb.h>
#include <stdio.h>

#include "bip32.h"
#include "bip39.h"
#include "checks.h"
#include "config.h"
#include "curves.h"
#include "init_wallet.h"
#include "log.h"
#include "protection.h"
#include "rand.h"
#include "rpc.h"
#include "transact.h"

extern NFast_AppHandle app;

static Result ticket2keyId(const uint8_t *ticket_bytes, uint32_t ticket_len, M_KeyID *key_id);
extern M_KeyID master_seed_encryption_key;
extern M_KeyID pub_key_encryption_key;

/**
 * Load keys using tickets.
 */
Result pre_execute_command(InternalCommandRequest *in) {
  DEBUG("in pre_execute_command");

  if (!in->has_master_seed_encryption_key_ticket) {
    ERROR("master_seed_encryption_key_ticket missing.");
    return Result_NO_MASTER_SEED_ENCRYPTION_KEY_TICKET_FAILURE;
  }
  if (!in->has_pub_key_encryption_key_ticket) {
    ERROR("pub_key_encryption_key_ticket missing.");
    return Result_NO_PUB_KEY_ENCRYPTION_KEY_TICKET_FAILURE;
  }

  int i;
  // 1. Load the master_seed_encryption_key using the ticket
  DEBUG("master_seed_encryption_key_ticket: %d",
        in->master_seed_encryption_key_ticket.size);
  for (i = 0; i < in->master_seed_encryption_key_ticket.size; i++) {
    DEBUG_("%02x", in->master_seed_encryption_key_ticket.bytes[i]);
  }
  DEBUG_("\n");

  Result r = ticket2keyId(in->master_seed_encryption_key_ticket.bytes,
                          in->master_seed_encryption_key_ticket.size,
                          &master_seed_encryption_key);
  if (r != Result_SUCCESS) {
    ERROR("ticket2keyId failed (%d).", r);
    master_seed_encryption_key = 0;
    return r;
  }
  INFO("master_seed_encryption_key loaded");

  // 2. Load the pub_key_encryption_key using the ticket
  DEBUG("pub_key_encryption_key_ticket: %d",
        in->pub_key_encryption_key_ticket.size);
  for (i = 0; i < in->pub_key_encryption_key_ticket.size; i++) {
    DEBUG_("%02x", in->pub_key_encryption_key_ticket.bytes[i]);
  }
  DEBUG_("\n");

  // Load the shared encryption key
  r = ticket2keyId(in->pub_key_encryption_key_ticket.bytes,
                   in->pub_key_encryption_key_ticket.size,
                   &pub_key_encryption_key);
  if (r != Result_SUCCESS) {
    ERROR("ticket2keyId failed (%d).", r);
    master_seed_encryption_key = 0;
    pub_key_encryption_key = 0;
    return r;
  }
  INFO("pub_key_encryption_key loaded");

  return Result_SUCCESS;
}

void post_execute_command(void) {
  master_seed_encryption_key = 0;
  pub_key_encryption_key = 0;
}

uint8_t ticket_buffer[256];
static Result ticket2keyId(const uint8_t *ticket_bytes, uint32_t ticket_len,
                           M_KeyID *key_id) {
  if (ticket_len > sizeof(ticket_buffer)) {
    ERROR("ticket len too large");
    return Result_TICKET_LEN_OVERFLOW_FAILURE;
  }
  M_Command command = {0};
  M_Reply reply = {0};
  M_ByteBlock ticket_block;
  Result r;

  ticket_block.len = ticket_len;
  memcpy(ticket_buffer, ticket_bytes, ticket_len);
  ticket_block.ptr = ticket_buffer;

  command.cmd = Cmd_RedeemTicket;
  command.args.redeemticket.ticket = ticket_block;

  r = transact(&command, &reply);
  if (r != Result_SUCCESS) {
    ERROR("ticket2keyId: transact failed.");
    NFastApp_Free_Reply(app, NULL, NULL, &reply);
    return r;
  }

  *key_id = reply.reply.redeemticket.obj;

  NFastApp_Free_Reply(app, NULL, NULL, &reply);
  return Result_SUCCESS;
}
