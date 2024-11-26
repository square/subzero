#include "aes_gcm_common.h"
#include "aes_gcm_ncipher.h"
#include "log.h"
#include "memzero.h"
#include "squareup/subzero/internal.pb.h" // For error codes
#include "transact.h"

#include <nfastapp.h>
#include <stdint.h>
#include <string.h>

#define IV_SIZE_IN_BYTES AES_GCM_IV_SIZE_IN_BYTES
#define TAG_SIZE_IN_BYTES AES_GCM_TAG_SIZE_IN_BYTES

extern NFast_AppHandle app;

// For some unknown reason, NFastApp_Transact with -O2 requires heap allocated
// buffers. 1000 bytes should be enough.
uint8_t aes_gcm_buffer[1000];

/**
 * AES-GCM encryption. Used to encrypt the wallet. Also used to encrypt the
 * public key. When this function returns, plaintext is zeroed out.
 *
 * Implementation detail:
 * Ciphertext is [IV (12 bytes), ciphertext (same length as input), TAG (16
 * bytes)].
 *
 * All pointer arguments of the function must not be NULL.
 */
Result aes_gcm_encrypt(
    M_KeyID keyId,
    uint8_t* plaintext,
    size_t plaintext_len,
    uint8_t* ciphertext,
    size_t ciphertext_len,
    size_t* bytes_written) {
  // NULL pointer checks.
  if (!plaintext) {
    ERROR("%s: plaintext must not be NULL", __func__);
    return Result_UNKNOWN_INTERNAL_FAILURE;
  }

  if (!ciphertext) {
    ERROR("%s: ciphertext must not be NULL", __func__);
    return Result_UNKNOWN_INTERNAL_FAILURE;
  }

  if (!bytes_written) {
    ERROR("%s: bytes_written must not be NULL", __func__);
    return Result_UNKNOWN_INTERNAL_FAILURE;
  }

  Result r;
  memzero(ciphertext, ciphertext_len);

  if (plaintext_len > SIZE_MAX - IV_SIZE_IN_BYTES - TAG_SIZE_IN_BYTES) {
    ERROR("%s: plaintext too long.", __func__);
    memzero(plaintext, plaintext_len);
    return Result_AES_GCM_ENCRYPT_PLAINTEXT_TOO_LONG_FAILURE;
  }

  size_t expected_ciphertext_len = plaintext_len + IV_SIZE_IN_BYTES + TAG_SIZE_IN_BYTES;
  if (ciphertext_len < expected_ciphertext_len) {
    ERROR("aes_gcm_encrypt: ciphertext buffer too small.");
    memzero(plaintext, plaintext_len);
    return Result_AES_GCM_ENCRYPT_BUFFER_TOO_SMALL_FAILURE;
  }

  if (expected_ciphertext_len > sizeof(aes_gcm_buffer)) {
    ERROR("aes_gcm_encrypt: plaintext too long.");
    memzero(plaintext, plaintext_len);
    return Result_AES_GCM_ENCRYPT_PLAINTEXT_TOO_LONG_FAILURE;
  }

  M_Command command = { 0 };
  M_Reply reply = { 0 };

  command.cmd = Cmd_Encrypt;
  command.args.encrypt.key = keyId;
  command.args.encrypt.mech = Mech_RijndaelmGCM;
  command.args.encrypt.plain.type = PlainTextType_Bytes;
  command.args.encrypt.given_iv = NULL;

  // Encrypt plaintext.
  memcpy(aes_gcm_buffer, plaintext, plaintext_len);
  memzero(plaintext, plaintext_len);

  command.args.encrypt.plain.data.bytes.data.len = plaintext_len;
  command.args.encrypt.plain.data.bytes.data.ptr = aes_gcm_buffer;

  r = transact(&command, &reply);
  memzero(aes_gcm_buffer, sizeof(aes_gcm_buffer));

  if (r != Result_SUCCESS) {
    ERROR("aes_gcm_encrypt: transact failed.");
    NFastApp_Free_Reply(app, NULL, NULL, &reply);
    return r;
  }

  if (reply.reply.encrypt.cipher.data.genericgcm128.cipher.len != plaintext_len) {
    ERROR("aes_gcm_encrypt: unexpected cipher len.");
    NFastApp_Free_Reply(app, NULL, NULL, &reply);
    return Result_AES_GCM_ENCRYPT_UNEXPECTED_CIPHERTEXT_LEN_FAILURE;
  }

  if (reply.reply.encrypt.cipher.iv.genericgcm128.iv.len != IV_SIZE_IN_BYTES) {
    ERROR("aes_gcm_encrypt: unexpected IV len.");
    NFastApp_Free_Reply(app, NULL, NULL, &reply);
    return Result_AES_GCM_ENCRYPT_UNEXPECTED_IV_LEN_FAILURE;
  }

  memcpy(ciphertext, reply.reply.encrypt.cipher.iv.genericgcm128.iv.ptr, IV_SIZE_IN_BYTES);
  memcpy(ciphertext + IV_SIZE_IN_BYTES, reply.reply.encrypt.cipher.data.genericgcm128.cipher.ptr, plaintext_len);
  memcpy(
      ciphertext + plaintext_len + IV_SIZE_IN_BYTES,
      reply.reply.encrypt.cipher.data.genericgcm128.tag.bytes,
      TAG_SIZE_IN_BYTES);
  *bytes_written = expected_ciphertext_len;

  NFastApp_Free_Reply(app, NULL, NULL, &reply);
  return Result_SUCCESS;
}

/**
 * AES-GCM decryption.
 */
Result aes_gcm_decrypt(
    M_KeyID keyId,
    const uint8_t* ciphertext,
    size_t ciphertext_len,
    uint8_t* plaintext,
    size_t plaintext_len,
    size_t* bytes_written) {
  // NULL pointer checks.
  if (!plaintext) {
    ERROR("%s: plaintext must not be NULL", __func__);
    return Result_UNKNOWN_INTERNAL_FAILURE;
  }

  if (!ciphertext) {
    ERROR("%s: ciphertext must not be NULL", __func__);
    return Result_UNKNOWN_INTERNAL_FAILURE;
  }

  if (!bytes_written) {
    ERROR("%s: bytes_written must not be NULL", __func__);
    return Result_UNKNOWN_INTERNAL_FAILURE;
  }

  memzero(plaintext, plaintext_len);

  if (ciphertext_len < IV_SIZE_IN_BYTES + TAG_SIZE_IN_BYTES) {
    ERROR("%s: ciphertext buffer too small.", __func__);
    return Result_AES_GCM_DECRYPT_BUFFER_TOO_SMALL_FAILURE;
  }

  if (ciphertext_len > sizeof(aes_gcm_buffer)) {
    ERROR("%s: ciphertext too long.", __func__);
    return Result_AES_GCM_DECRYPT_CIPHERTEXT_TOO_LONG_FAILURE;
  }

  size_t expected_plaintext_len = ciphertext_len - IV_SIZE_IN_BYTES - TAG_SIZE_IN_BYTES;
  if (plaintext_len < expected_plaintext_len) {
    ERROR("aes_gcm_decrypt: plaintext buffer too small.");
    return Result_AES_GCM_DECRYPT_BUFFER_TOO_SMALL_FAILURE;
  }
  if (ciphertext_len > sizeof(aes_gcm_buffer)) {
    ERROR("aes_gcm_decrypt: ciphertext too long.");
    return Result_AES_GCM_DECRYPT_CIPHERTEXT_TOO_LONG_FAILURE;
  }

  M_Command command = { 0 };
  M_Reply reply = { 0 };
  Result r;

  command.cmd = Cmd_Decrypt;
  command.args.decrypt.key = keyId;
  command.args.decrypt.mech = Mech_RijndaelmGCM;
  command.args.decrypt.reply_type = PlainTextType_Bytes;

  memcpy(aes_gcm_buffer, ciphertext, ciphertext_len);
  command.args.decrypt.cipher.mech = Mech_RijndaelmGCM;
  command.args.decrypt.cipher.data.genericgcm128.cipher.len = expected_plaintext_len;
  command.args.decrypt.cipher.data.genericgcm128.cipher.ptr = aes_gcm_buffer + IV_SIZE_IN_BYTES;
  memcpy(
      command.args.decrypt.cipher.data.genericgcm128.tag.bytes,
      aes_gcm_buffer + ciphertext_len - TAG_SIZE_IN_BYTES,
      TAG_SIZE_IN_BYTES);
  command.args.decrypt.cipher.iv.genericgcm128.taglen = TAG_SIZE_IN_BYTES;
  command.args.decrypt.cipher.iv.genericgcm128.iv.len = IV_SIZE_IN_BYTES;
  command.args.decrypt.cipher.iv.genericgcm128.iv.ptr = aes_gcm_buffer;
  command.args.decrypt.cipher.iv.genericgcm128.header.len = 0;

  r = transact(&command, &reply);
  if (r != Result_SUCCESS) {
    ERROR("aes_gcm_decrypt: transact failed.");
    NFastApp_Free_Reply(app, NULL, NULL, &reply);
    return r;
  }

  if (reply.reply.decrypt.plain.data.bytes.data.len != expected_plaintext_len) {
    ERROR("aes_gcm_decrypt: invalid plain len.");
    NFastApp_Free_Reply(app, NULL, NULL, &reply);
    return Result_AES_GCM_DECRYPT_UNEXPECTED_PLAINTEXT_LEN_FAILURE;
  }

  // success!
  memcpy(plaintext, reply.reply.decrypt.plain.data.bytes.data.ptr, expected_plaintext_len);

  *bytes_written = expected_plaintext_len;

  NFastApp_Free_Reply(app, NULL, NULL, &reply);
  return Result_SUCCESS;
}
