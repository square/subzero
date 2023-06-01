#include "checks.h"
#include "config.h"
#include "log.h"
#include "memzero.h"
#include "pb.h"
#include "rand.h"
#include "rpc.h"
#include "squareup/subzero/internal.pb.h"

#include <assert.h>
#include <pb_decode.h>
#include <pb_encode.h>
#include <stdbool.h>
#include <string.h>

// Helper which returns the size of a buffer that would be needed to hold the serialized version of the given
// protobuf structure, assuming that pb_encode_delimited() serialization will be used.
static size_t get_serialized_proto_struct_size(const pb_field_t fields[], const void* const proto_struct) {
  pb_ostream_t stream = PB_OSTREAM_SIZING;
  if (!pb_encode_delimited(&stream, fields, proto_struct)) {
    ERROR("%s: pb_encode_delimited() failed: %s", __func__, PB_GET_ERROR(&stream));
    return 0;
  }
  return stream.bytes_written;
}

// Serializes the given protobuf structure to the given buffer of the given size, using pb_encode_delimited().
// Returns true on success or false on failure.
// Caller brings their own buffer memory, this function does not allocate.
static bool serialize_proto_struct_to_buffer(
    pb_byte_t* const buffer,
    const size_t buffer_size,
    const pb_field_t fields[],
    const void* const proto_struct) {
  pb_ostream_t ostream = pb_ostream_from_buffer(buffer, buffer_size);
  if (!pb_encode_delimited(&ostream, fields, proto_struct)) {
    ERROR("%s: pb_encode_delimited() failed: %s", __func__, PB_GET_ERROR(&ostream));
    return false;
  }
  return true;
}

// Deserializes the given buffer into the given protobuf structure, using pb_decode_delimited().
// Returns true on success or false on failure.
// Caller brings their own protobuf structure, this function does not allocate.
static bool deserialize_proto_struct_from_buffer(
    const pb_byte_t* const buffer,
    const size_t buffer_size,
    const pb_field_t fields[],
    void* const proto_struct) {
  pb_istream_t istream = pb_istream_from_buffer(buffer, buffer_size);
  if (!pb_decode_delimited(&istream, fields, proto_struct)) {
    ERROR("%s: pb_decode_delimited() failed: %s", __func__, PB_GET_ERROR(&istream));
    return false;
  }
  return true;
}

/**
 * Helper for checking assumptions in the gnarly protobuf mangling code in verify_rpc_oversized_message_rejected().
 */
static bool check_byte_equals(const char* parent_func, const pb_byte_t* const buf, size_t idx, pb_byte_t expected_val) {
  const pb_byte_t actual_val = buf[idx];
  if (actual_val != expected_val) {
    ERROR(
        "%s: buf[%zu] contains an unexpected value: %hhu, expected: %hhu", parent_func, idx, actual_val, expected_val);
    return false;
  }
  return true;
}

static pb_byte_t request_buffer[256] = { 0 };
static pb_byte_t response_buffer[256] = { 0 };

int verify_rpc_oversized_message_rejected(void) {
  int result = 0;

  // Construct an initial InternalCommandRequest which holds an InitWallet command
  // with a maximum-allowed-length random_bytes field.
  InternalCommandRequest cmd = InternalCommandRequest_init_default;
  cmd.version = VERSION;
  cmd.wallet_id = 1; // dummy value
  cmd.which_command = InternalCommandRequest_InitWallet_tag;
  static_assert(
      sizeof(cmd.command.InitWallet.random_bytes.bytes) == MASTER_SEED_SIZE,
      "MASTER_SEED_SIZE must equal sizeof(cmd.command.InitWallet.random_bytes.bytes)");
  cmd.command.InitWallet.random_bytes.size = MASTER_SEED_SIZE;
  random_buffer(cmd.command.InitWallet.random_bytes.bytes, MASTER_SEED_SIZE);

  // Compute the size of the serialized struct.
  size_t serialized_size = get_serialized_proto_struct_size(InternalCommandRequest_fields, &cmd);
  if (serialized_size == 0) {
    ERROR("%s: error computing serialized request size", __func__);
    result = -1;
    goto out;
  }

  if (serialized_size + 1 > sizeof(request_buffer)) {
    ERROR(
        "%s: sizeof(request_buffer) == %zu but needs to be at least %zu. Modify the code and rebuild.",
        __func__,
        sizeof(request_buffer),
        serialized_size + 1);
    result = -1;
    goto out;
  }

  // Serialize the request struct into request_buffer.
  if (!serialize_proto_struct_to_buffer(request_buffer, sizeof(request_buffer), InternalCommandRequest_fields, &cmd)) {
    ERROR("%s: serialize_proto_struct_to_buffer() failed", __func__);
    result = -1;
    goto out;
  }

  // Corrupt the message by making the random_bytes field 1 byte longer than the max allowed size.
  // Note that this is a bit fragile and could break if the protobuf definitions inside
  // internal.proto are changed. But if that happens, hopefully this test breaks immediately
  // and can be fixed. Understanding of low-level protobuf serialization is recommended, see
  // https://protobuf.dev/programming-guides/encoding/ for the details (it's not that bad).
  // Basically:
  //   serialized_request[0] - varint-encoded leading LEN byte. This is not normally there for binary
  //                           encoded protobufs, but it's added by nanopb because we are using
  //                           pb_encode_delimited(). If the message is longer than 127 bytes, this
  //                           length will actually take more than 1 byte, shifting everything after
  //                           it by a byte.
  //                           *** NOTE: WE NEED TO INCREMENT THIS BY 1. ***
  //   serialized_request[1] - field id (1 << 3) + tag (0) for field 1 (version). Should equal 8.
  //   serialized_request[2..3] - varint-encoded value for field 1. Leave this alone, it's the
  //                              contents of the 'version' field (210 at the time of writing). If
  //                              version ever exceeds 16383, this will start taking up an extra byte
  //                              and shift everything after it by a byte.
  //   serialized_request[4] - field id (2 << 3) + tag (0) for field 2 (wallet_id). Should equal 16.
  //   serialized_request[5] - varint-encoded value for field 2. Leave this alone, it's the dummy
  //                           'wallet' field which we set to 1 above. Should equal 1.
  //   serialized_request[6] - field id (5 << 3) + tag (2, for 'LEN') for field 5 (command.InitWallet).
  //                           Should equal 42.
  //   serialized_request[7] - varint-encoded LEN of the InitWalletRequest submessage.
  //                           Should equal 66.
  //                           *** NOTE: WE NEED TO INCREMENT THIS BY 1. ***
  //   serialized_request[8] - field id (1 << 3) + tag (2, for 'LEN') for field 1 of sub-message.
  //                           Should equal 10.
  //   serialized_request[9] - varint-encoded LEN of field 1 (random_bytes) of sub-message.
  //                           Should equal 64.
  //                           *** NOTE: WE NEED TO INCREMENT THIS BY 1. ***
  //   serialized_request[10..73] - the contents of the random_bytes field. Should be 64 bytes in length.
  //   serialized_request[74] - doesn't exist in the original message. We add an extra data byte here.
  //                            It can be any value, we arbitrarily choose 0xaa.
  //
  // Let's check the above assumptions to make sure they are correct before proceeding:
  if (!check_byte_equals(__func__, request_buffer, 0, (pb_byte_t) 73)) {
    result = -1;
    goto out;
  }
  if (!check_byte_equals(__func__, request_buffer, 0, (pb_byte_t) (serialized_size - 1))) {
    result = -1;
    goto out;
  }
  if (!check_byte_equals(__func__, request_buffer, 1, (pb_byte_t) ((1 << 3) + 0))) {
    result = -1;
    goto out;
  }
  // The 'cmd.version' field is varint-encoded into 2 little-endian bytes:
  // ... First byte contains least-significant 7 bits + highest bit set to indicate that there's more data.
  if (!check_byte_equals(__func__, request_buffer, 2, (pb_byte_t) ((cmd.version & 0x7f) | 0x80))) {
    result = -1;
    goto out;
  }
  // ... Second byte contains the next 1-7 bits + highest bit unset to indicate that there's no more data.
  if (!check_byte_equals(__func__, request_buffer, 3, (pb_byte_t) (cmd.version >> 7))) {
    result = -1;
    goto out;
  }
  if (!check_byte_equals(__func__, request_buffer, 4, (pb_byte_t) ((2 << 3) + 0))) {
    result = -1;
    goto out;
  }
  if (!check_byte_equals(__func__, request_buffer, 5, (pb_byte_t) cmd.wallet_id)) {
    result = -1;
    goto out;
  }
  if (!check_byte_equals(__func__, request_buffer, 6, (pb_byte_t) ((5 << 3) + 2))) {
    result = -1;
    goto out;
  }
  if (!check_byte_equals(__func__, request_buffer, 7, (pb_byte_t) (MASTER_SEED_SIZE + 2))) {
    result = -1;
    goto out;
  }
  if (!check_byte_equals(__func__, request_buffer, 8, (pb_byte_t) ((1 << 3) + 2))) {
    result = -1;
    goto out;
  }
  if (!check_byte_equals(__func__, request_buffer, 9, (pb_byte_t) MASTER_SEED_SIZE)) {
    result = -1;
    goto out;
  }

  request_buffer[0]++;                    // increment leading LEN byte
  request_buffer[7]++;                    // increment LEN byte for top-level field 5
  request_buffer[9]++;                    // increment LEN byte for nested field 1
  request_buffer[serialized_size] = 0xaa; // set the last byte to an arbitrary value
  serialized_size++;                      // increment serialized_size since we added a byte of data

  // Create a stream which will read from the corrupted serialized buffer.
  pb_istream_t istream = pb_istream_from_buffer(request_buffer, serialized_size);
  // Create a stream which will write to the response buffer.
  pb_ostream_t ostream = pb_ostream_from_buffer(response_buffer, sizeof(response_buffer));

  // Now that we have a serialized buffer, try to pass it to handle_incoming_message().
  // This should fail because the InitWallet.random_bytes field has a length of 65 bytes,
  // but nanopb options set the limit for this field at 64 bytes.
  //
  // NOTE: when building for nCipher, there are command hooks that would reject the command
  // because it's missing the tickets for key use authorization. But this doesn't matter for
  // this test case, because the protobuf parsing happens before that and fails first.
  ERROR("(next line is expected to show red text...)");

  handle_incoming_message(&istream, &ostream); // <---- this is the actual function under test

  // Extract the response structure from the serialized_response buffer. It should be an error.
  const size_t response_size = ostream.bytes_written;
  if (response_size == 0) {
    ERROR("%s: no response received from handle_incoming_message(): %s", __func__, PB_GET_ERROR(&ostream));
    result = -1;
    goto out;
  }

  // note: no need to initialize the response, static bool deserialize_proto_struct_from_buffer() does it via
  // pb_decode_delimited().
  InternalCommandResponse response;
  if (!deserialize_proto_struct_from_buffer(
          response_buffer, response_size, InternalCommandResponse_fields, &response)) {
    ERROR("%s: deserialize_proto_struct_from_buffer() failed", __func__);
    result = -1;
    goto out;
  }

  // Check that the response contains an error.
  if (response.which_response != InternalCommandResponse_Error_tag) {
    ERROR(
        "%s: wrong response tag: %d, expected: %d",
        __func__,
        (int) response.which_response,
        (int) InternalCommandResponse_Error_tag);
    result = -1;
    goto out;
  }

  // Check that the error response contains the expected error code.
  if (response.response.Error.code != Result_COMMAND_DECODE_FAILED) {
    ERROR(
        "%s: wrong response error code: %d, expected: %d",
        __func__,
        (int) response.response.Error.code,
        (int) Result_COMMAND_DECODE_FAILED);
    result = -1;
    goto out;
  }

  // Check that the error response contains some message.
  if (!response.response.Error.has_message) {
    ERROR("%s: error response does not contain a 'message' field", __func__);
    result = -1;
    goto out;
  }

  // Check that the error response contains the expected message.
  if (0 != strcmp("Decode Input failed: bytes overflow", response.response.Error.message)) {
    ERROR("%s: error response contains unexpected message: %s", __func__, response.response.Error.message);
    result = -1;
    goto out;
  }

out:
  memzero(request_buffer, sizeof(request_buffer));
  memzero(response_buffer, sizeof(response_buffer));

  return result;
}
