#pragma once

// Common definitions included by both aes_gcm_dev.h and aes_gcm_ncipher.h

#define AES_GCM_IV_SIZE_IN_BYTES (12)
#define AES_GCM_TAG_SIZE_IN_BYTES (16)
#define AES_GCM_OVERHEAD_BYTES (AES_GCM_IV_SIZE_IN_BYTES + AES_GCM_TAG_SIZE_IN_BYTES)
