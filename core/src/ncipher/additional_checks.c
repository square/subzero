#include <nfastapp.h>
#include <seelib.h>
#include <stdint.h>

#include "aes_gcm.h"
#include "checks.h"
#include "log.h"
#include "transact.h"

extern M_KeyID master_seed_encryption_key;
extern M_KeyID pub_key_encryption_key;

M_PermissionGroup check_aes_gcm_pg = {0};
M_Action check_aes_gcm_act = {0};

static int check_aes_gcm(void);

/**
 * nCipher specific setup.
 */
int pre_run_self_checks(void) {
  // Create a temporary key, assign it to master_seed_encryption_key and
  // pub_key_encryption_key
  M_Command command = {0};
  M_Reply reply = {0};
  Result r;

  check_aes_gcm_act.type = Act_OpPermissions;
  check_aes_gcm_act.details.oppermissions.perms =
      (Act_OpPermissions_Details_perms_Encrypt |
       Act_OpPermissions_Details_perms_Decrypt);

  check_aes_gcm_pg.flags = 0;
  check_aes_gcm_pg.n_limits = 0;
  check_aes_gcm_pg.n_actions = 1;
  check_aes_gcm_pg.actions = &check_aes_gcm_act;

  command.cmd = Cmd_GenerateKey;
  command.args.generatekey.flags = 0;
  command.args.generatekey.params.type = KeyType_Rijndael;
  command.args.generatekey.params.params.random.lenbytes = 16; // 128 bit key
  command.args.generatekey.acl.n_groups = 1;
  command.args.generatekey.acl.groups = &check_aes_gcm_pg;
  command.args.generatekey.appdata = NULL;

  r = transact(&command, &reply);
  if (r != Result_SUCCESS) {
    ERROR("pre_run_self_checks: transact failed");
    return -1;
  }

  master_seed_encryption_key = reply.reply.generatekey.key;
  pub_key_encryption_key = reply.reply.generatekey.key;

  return 0;
}

/**
 * nCipher specific cleanup.
 */
int post_run_self_checks(void) {
  // TODO: implement AES in dev and move this test out of here.
  int r = check_aes_gcm();

  master_seed_encryption_key = 0;
  pub_key_encryption_key = 0;

  return r;
}

/**
 * Encrypt/decrypt some bytes.
 *
 */
static int check_aes_gcm(void) {
  // encrypt some bytes twice.
  uint8_t plaintext[40] = {0x1a, 0x44, 0x2b, 0x56, 0x83, 0xa9, 0xa4, 0xd2,
                           0x98, 0x1d, 0x2b, 0x29, 0x3a, 0x03, 0x93, 0xaa,
                           0x0e, 0x78, 0xda, 0x47, 0x4e, 0x07, 0x36, 0x15,
                           0xfc, 0x49, 0x97, 0xdd, 0x8f, 0x0f, 0x1b, 0xaa,
                           0xd7, 0x63, 0x27, 0x72, 0x23, 0x0a, 0x49, 0x08};
  uint8_t plaintext2[40] = {0};
  memcpy(plaintext2, plaintext, sizeof(plaintext));

  uint8_t ciphertext1[100] = {0};
  uint8_t ciphertext2[100] = {0};
  uint8_t roundtrip[40] = {0};

  size_t ciphertext1_len, ciphertext2_len, roundtrip_len;
  Result r =
      aes_gcm_encrypt(master_seed_encryption_key, plaintext, sizeof(plaintext),
                      ciphertext1, sizeof(ciphertext1), &ciphertext1_len);
  if (r != Result_SUCCESS) {
    ERROR("first encryption failed (%d).", r);
    return -1;
  }

  r = aes_gcm_encrypt(master_seed_encryption_key, plaintext, sizeof(plaintext),
                      ciphertext2, sizeof(ciphertext2), &ciphertext2_len);
  if (r != Result_SUCCESS) {
    ERROR("second encryption failed (%d).", r);
    return -1;
  }

  if (ciphertext1_len != ciphertext2_len) {
    ERROR("unexpected ciphertext lengths");
    return -1;
  }

  // check that we got different bytes.
  if (memcmp(ciphertext1, ciphertext2, ciphertext1_len) == 0) {
    ERROR("encryption failed to generate random IV.");
    return -1;
  }

  // check that we can decrypt back to the original string.
  r = aes_gcm_decrypt(master_seed_encryption_key, ciphertext1, ciphertext1_len,
                      roundtrip, sizeof(roundtrip), &roundtrip_len);
  if (r != Result_SUCCESS) {
    ERROR("first decryption failed (%d).", r);
    return -1;
  }

  if (roundtrip_len != sizeof(plaintext2)) {
    ERROR("unexpected roundtrip length");
    return -1;
  }
  if (memcmp(plaintext2, roundtrip, roundtrip_len) != 0) {
    ERROR("roundtrip mismatch");
    return -1;
  }

  // check that modifying a byte causes decryption to fail.
  ciphertext2[10] = ciphertext2[10] ^ 0x22;
  ERROR("expecting aes_gcm_decrypt to fail.");
  r = aes_gcm_decrypt(master_seed_encryption_key, ciphertext2, ciphertext2_len,
                      roundtrip, sizeof(roundtrip), &roundtrip_len);
  if (r == Result_SUCCESS) {
    ERROR("decryption of corrupted bytes should not have worked.");
    return -1;
  }

  return 0;
}
