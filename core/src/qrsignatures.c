#include "qrsignatures.h"

#include "config.h"
#include "ecdsa.h"
#include "hasher.h"
#include "log.h"
#include "nist256p1.h"

#include <assert.h>
#include <stdbool.h>

static const uint8_t QR_PUBKEY[QRSIGNATURE_PUBKEY_LEN] = _QR_PUBKEY;

bool check_qrsignature(
    const uint8_t* const data,
    const size_t data_len,
    const uint8_t* const signature,
    const size_t signature_len) {
  static_assert(sizeof(QR_PUBKEY) == QRSIGNATURE_PUBKEY_LEN, "sizeof(QR_PUBKEY) must equal QRSIGNATURE_PUBKEY_LEN");
  return check_qrsignature_pub(data, data_len, signature, signature_len, QR_PUBKEY, sizeof(QR_PUBKEY));
}

bool check_qrsignature_pub(
    const uint8_t* const data,
    const size_t data_len,
    const uint8_t* const signature,
    const size_t signature_len,
    const uint8_t* const pubkey,
    const size_t pubkey_len) {
  if (data_len == 0) {
    ERROR("Input length is zero.");
    return false;
  }
  if (data == NULL) {
    ERROR("Input data is null.");
    return false;
  }
  if (signature_len != QRSIGNATURE_LEN) {
    ERROR("%s: signature_len is: %zu, expected: %zu", __func__, signature_len, QRSIGNATURE_LEN);
    return false;
  }
  if (signature == NULL) {
    ERROR("Signature is null");
    return false;
  }
  if (pubkey_len != QRSIGNATURE_PUBKEY_LEN) {
    ERROR("%s: pubkey_len is: %zu, expected: %zu", __func__, pubkey_len, QRSIGNATURE_PUBKEY_LEN);
    return false;
  }
  if (pubkey == NULL) {
    ERROR("pub is null.");
    return false;
  }

  int result_verify = ecdsa_verify(
    (const ecdsa_curve *)&nist256p1,
    HASHER_SHA2,
    (const uint8_t *) pubkey,
    signature,
    data,
    (uint32_t)data_len);

  if(result_verify == 0){
      DEBUG("QR signature verification successful.");
  } else {
      DEBUG("QR signature verification failed.");
  }
  return (result_verify == 0)? true: false;
}
