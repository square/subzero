#include "script.h"

#include "squareup/subzero/internal.pb.h"

#include <log.h>

Result script_push(script_t* script, enum opcodetype opcode) {
  if (script->len == SCRIPT_MAX_LEN) {
    return Result_SCRIPT_PUSH_OVERFLOW_FAILURE;
  }
  script->data[script->len] = opcode;
  script->len++;
  return Result_SUCCESS;
}

Result script_push_data(script_t* script, const uint8_t* data, size_t len) {
  Result r;
  if (len < OP_PUSHDATA1) {
    r = script_push(script, len);
    if (r != Result_SUCCESS) {
      ERROR("script_push failed: (%d).", r);
      return r;
    }
  } else if (len <= 0xff) {
    r = script_push(script, OP_PUSHDATA1);
    if (r != Result_SUCCESS) {
      ERROR("script_push failed: (%d).", r);
      return r;
    }
    r = script_push(script, len);
    if (r != Result_SUCCESS) {
      ERROR("script_push failed: (%d).", r);
      return r;
    }
  } else if (len <= 0xffff) {
    r = script_push(script, OP_PUSHDATA2);
    if (r != Result_SUCCESS) {
      ERROR("script_push failed: (%d).", r);
      return r;
    }
    // TODO: handle endianess!
    return Result_SCRIPT_PUSH_UNIMPLEMENTED;
  } else {
    r = script_push(script, OP_PUSHDATA4);
    if (r != Result_SUCCESS) {
      ERROR("script_push failed: (%d).", r);
      return r;
    }
    // TODO: handle endianess!
    return Result_SCRIPT_PUSH_UNIMPLEMENTED;
  }
  size_t i;
  for (i = 0; i < len; i++) {
    r = script_push(script, data[i]);
    if (r != Result_SUCCESS) {
      ERROR("script_push failed: (%d).", r);
      return r;
    }
  }
  return Result_SUCCESS;
}
