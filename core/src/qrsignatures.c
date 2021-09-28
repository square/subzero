#include <unistd.h>
#include <stdbool.h>
#include <string.h>
#include <stdio.h>

#include "ecdsa.h"
#include "curves.h"
#include "memzero.h"
#include "nist256p1.h"
#include "qrsignatures.h"
#include "log.h"
#include "config.h"

static const uint8_t QR_PUBKEY[65] = _QR_PUBKEY;
bool check_qrsignature(const uint8_t * const data, size_t data_len, const uint8_t * const signature){
  return check_signature_pub(data, data_len, signature, QR_PUBKEY);
}

bool check_signature_pub(const uint8_t * const data, size_t data_len, const uint8_t * const signature, const uint8_t * const pub){

  if (data_len == 0){
    ERROR("Input length is zero.");
    return false;
  }
  if (data == NULL){
    ERROR("Input data is null.");
    return false;
  }
  if(signature == NULL){
    ERROR("Signature is null");
    return false;
  }

  
  int result_verify = ecdsa_verify(
    (const ecdsa_curve *)&nist256p1,
    HASHER_SHA2,
    (const uint8_t *)pub,
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
