#include "script.h"
#include "conv.h"

#include <limits.h>
#include <log.h>
#include <squareup/subzero/internal.pb.h>

Result script_push(script_t *script, uint8_t byte) {
  if (script->len == SCRIPT_MAX_LEN) {
    return Result_SCRIPT_PUSH_OVERFLOW_FAILURE;
  }
  script->data[script->len] = byte;
  script->len++;
  return Result_SUCCESS;
}

Result script_push_data(script_t *script, const uint8_t *data, size_t len) {
  Result r;
  if (len < OP_PUSHDATA1) {
    r = script_push(script, (uint8_t) len);
    if (r != Result_SUCCESS) {
      ERROR("script_push failed: (%d).", r);
      return r;
    }
    // Fall through to pushing the data.
  } else if (len <= UINT8_MAX) {
    r = script_push(script, (uint8_t) OP_PUSHDATA1);
    if (r != Result_SUCCESS) {
      ERROR("script_push failed: (%d).", r);
      return r;
    }
    r = script_push(script, (uint8_t) len);
    if (r != Result_SUCCESS) {
      ERROR("script_push failed: (%d).", r);
      return r;
    }
    // Fall through to pushing the data.
  } else if (len <= UINT16_MAX) {
    r = script_push(script, (uint8_t) OP_PUSHDATA2);
    if (r != Result_SUCCESS) {
      ERROR("script_push failed: (%d).", r);
      return r;
    }
    uint8_t len_buf[sizeof(uint16_t)];
    if (!u16_to_little_endian_bytes((uint16_t) len, len_buf, sizeof(len_buf))) {
      ERROR("script_push failed after OP_PUSHDATA2");
      return Result_UNKNOWN_INTERNAL_FAILURE;
    }
    for (size_t i = 0; i < sizeof(len_buf); ++i) {
      r = script_push(script, (uint8_t) len_buf[i]);
      if (r != Result_SUCCESS) {
        ERROR("script_push failed: (%d).", r);
        return r;
      }
    }
    // Fall through to pushing the data.
  } else {
    r = script_push(script, (uint8_t) OP_PUSHDATA4);
    if (r != Result_SUCCESS) {
      ERROR("script_push failed: (%d).", r);
      return r;
    }
    uint8_t len_buf[sizeof(uint32_t)];
    if (!u32_to_little_endian_bytes((uint32_t) len, len_buf, sizeof(len_buf))) {
      ERROR("script_push failed after OP_PUSHDATA4");
      return Result_UNKNOWN_INTERNAL_FAILURE;
    }
    for (size_t i = 0; i < sizeof(len_buf); ++i) {
      r = script_push(script, (uint8_t) len_buf[i]);
      if (r != Result_SUCCESS) {
        ERROR("script_push failed: (%d).", r);
        return r;
      }
    }
    // Fall through to pushing the data.
  }

  // All of the branches above fall through to here on success.
  for (size_t i = 0; i < len; i++) {
    r = script_push(script, (uint8_t) data[i]);
    if (r != Result_SUCCESS) {
      ERROR("script_push failed: (%d).", r);
      return r;
    }
  }
  return Result_SUCCESS;
}
